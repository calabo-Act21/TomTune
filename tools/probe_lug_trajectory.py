#!/usr/bin/env python3
"""Offline hypothesis from metadata only; never imported by the Android app.
Observe adjacent-window trajectories, retain all competitors, and abstain before consensus.
This cannot establish which acoustic mode should be used near a lug.
"""
import argparse
import copy
import json
import math
from pathlib import Path
from summarize_mode_comparison import fields


def cents(a, b):
    return 1200 * math.log2(a / b)


def trajectories(windows, adjacent):
    if len(windows) != 3:
        return []
    eligible = [[p['hz'] for p in w['peaks'] if p['eligible']] for w in windows]
    tracks = []
    for seed in eligible[0]:
        track = [seed]
        for peaks in eligible[1:]:
            anchor = track[-1] if adjacent else seed
            candidate = min(peaks, key=lambda hz: abs(cents(hz, anchor)), default=None)
            if candidate is None or abs(cents(candidate, anchor)) > 35:
                break
            track.append(candidate)
        if len(track) == 3:
            tracks.append(track)
    return tracks


class Observer:
    """No first-peak lock: need several unique coherent trajectories; missing/competing resets."""
    def __init__(self):
        self.values = []

    def accept(self, windows):
        tracks = trajectories(windows, adjacent=True)
        if len(tracks) != 1:
            self.values.clear()
            return {'decision': 'abstain_missing' if not tracks else 'abstain_competing', 'trajectories': tracks}
        # Hypothesis: compare the late window after the transient/glide. Not a calibrated mode choice.
        hz = tracks[0][-1]
        self.values = (self.values + [hz])[-5:]
        spread = cents(max(self.values), min(self.values))
        if spread > 18 + 1e-8:
            self.values = [hz]
        elif (len(self.values) >= 3 and spread <= 8 + 1e-8) or len(self.values) >= 5:
            return {'decision': 'coherent_metadata_only', 'lateHz': sorted(self.values)[len(self.values)//2],
                    'count': len(self.values), 'trajectories': tracks}
        return {'decision': 'abstain_need_more', 'count': len(self.values), 'trajectories': tracks}


def injected_windows(text, impact):
    windows = []
    for line in text.splitlines():
        f = fields(line)
        if f.get('phase') == 'paired' and f.get('config') == 'current' and f.get('impact') == str(impact) and ' Window ' in line:
            peaks = []
            for item in f['peaks'].split(';'):
                hz, power, db, eligible = item.split(',')
                peaks.append({'hz': float(hz), 'eligible': eligible == 'true'})
            windows.append({'index': int(f['index']), 'peaks': peaks})
    return windows


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('results', type=Path)
    p.add_argument('injected_trace', type=Path)
    a = p.parse_args()
    results = json.loads(a.results.read_text())
    observer = Observer()
    firm = []
    for row in results['blocks']['firm']['impacts']:
        windows = row['diagnostic']['windows']
        old = trajectories(windows, adjacent=False)
        new = observer.accept(windows)
        assert not old and len(new['trajectories']) == 1
        firm.append({'impact': row['impact'], 'fixedAnchorTrajectories': old, **new})
    assert [r['decision'] for r in firm] == ['abstain_need_more', 'abstain_need_more', 'coherent_metadata_only', 'coherent_metadata_only']
    ambiguous = injected_windows(a.injected_trace.read_text(), 4)
    verdict = observer.accept(ambiguous)
    assert verdict['decision'] == 'abstain_competing' and len(verdict['trajectories']) >= 3
    missing = copy.deepcopy(results['blocks']['firm']['impacts'][0]['diagnostic']['windows'])
    missing[1]['peaks'] = []
    vanished = observer.accept(missing)
    assert vanished['decision'] == 'abstain_missing'
    assert observer.accept(results['blocks']['firm']['impacts'][0]['diagnostic']['windows'])['decision'] == 'abstain_need_more'
    print(json.dumps({'proposalOnly': True, 'source': 'retrospective metadata, not PCM replay',
                      'acousticModeCorrectness': 'open; no independent tuner',
                      'productBehaviorChanged': False,
                      'firmPhysicalMetadataProbe': firm,
                      'ambiguous491699801InjectedMetadataControl': verdict,
                      'missingWindowMetadataSimulationControl': vanished,
                      'physicalFiveStrokeCriterion': 'not met; only four detections for ten declared strokes, third detection follows cue six'},
                     ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
