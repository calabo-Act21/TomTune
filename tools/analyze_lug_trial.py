#!/usr/bin/env python3
"""Metadata-only physical-trial audit. Cue buckets never establish actual physical strokes.
Block membership uses the device's monotonic clock, including when a begin marker precedes capture.
"""
import argparse
import json
import math
import statistics
from collections import Counter, defaultdict
from pathlib import Path
from summarize_mode_comparison import fields, summarize


def analyze(text, operator):
    base = summarize(text)
    lines = text.splitlines()
    clocks = {(c.get('run'), c.get('generation')): c for c in base['clocks']}
    diagnostics = defaultdict(lambda: {'windows': [], 'modes': []})
    for line in lines:
        f = fields(line)
        if f.get('phase') == 'paired' and f.get('config') == 'current':
            key = (f.get('run'), f.get('generation'), int(f['impact']))
            if ' Attack ' in line:
                diagnostics[key]['attack'] = {k: float(f[k]) for k in ('rms20ms', 'peak20ms', 'clippedFraction')}
            if ' Window ' in line:
                peaks = []
                for raw in f['peaks'].split(';'):
                    if raw:
                        hz, power, relative, eligible = raw.split(',')
                        peaks.append({'hz': float(hz), 'power': float(power), 'relativeDb': float(relative), 'eligible': eligible == 'true'})
                diagnostics[key]['windows'].append({'index': int(f['index']), 'concentration': float(f['concentration']), 'peaks': peaks})
            if ' Mode ' in line:
                diagnostics[key]['modes'].append(f)
    for row in base['impacts']:
        clock = clocks[(row['run'], row['generation'])]
        row['onsetMonotonicNanos'] = int(clock['originMonotonicNanos']) + row['frame'] / row['rate'] * 1e9
        row['diagnostic'] = diagnostics[(row['run'], row['generation'], row['impact'])]
    blocks, used = {}, set()
    for block in ('light', 'firm'):
        marks = [m for m in base['markers'] if m.get('block') == block]
        begins = [m for m in marks if m.get('command') == 'begin']
        ends = [m for m in marks if m.get('command') == 'end']
        assert len(begins) == len(ends) == 1, 'One begin/end pair required'
        start, end = int(begins[0]['monotonicNanos']), int(ends[0]['monotonicNanos'])
        cues = sorted([m for m in marks if m.get('command') == 'cue'], key=lambda m: int(m['ordinal']))
        assert [int(m['ordinal']) for m in cues] == list(range(1, 11))
        cue_times = [int(m['monotonicNanos']) for m in cues]
        rows = [r for r in base['impacts'] if start <= r['onsetMonotonicNanos'] <= end]
        configurations = {}
        for config in ('lot0', 'current', 'band80_700'):
            results = [r['native'][config] or {} for r in rows]
            configurations[config] = {'native_detected': sum(r['native_detection_frames'][config] is not None for r in rows),
                                      'native_accepted': sum('hz' in s for s in results),
                                      'native_rejections': dict(Counter(s['reason'] for s in results if 'reason' in s)),
                                      'paired_accepted': sum('hz' in (r['paired'][config] or {}) for r in rows),
                                      'paired_rejections': dict(Counter(r['paired'][config]['reason'] for r in rows if 'reason' in (r['paired'][config] or {})))}
        for row in rows:
            used.add((row['run'], row['generation'], row['impact']))
            row['strict_cue_matches'] = [{'ordinal': i + 1, 'offsetSeconds': (row['onsetMonotonicNanos'] - t) / 1e9}
                                       for i, t in enumerate(cue_times) if -.15 <= (row['onsetMonotonicNanos'] - t) / 1e9 <= 1.20]
            # Broad intervals are retained as temporal buckets only; no retrospective "played stroke" claim.
            preceding = [i for i, t in enumerate(cue_times) if t <= row['onsetMonotonicNanos']]
            row['precedingCueOrdinal'] = preceding[-1] + 1 if preceding else None
            row['precedingCueOffsetSeconds'] = (row['onsetMonotonicNanos'] - cue_times[preceding[-1]]) / 1e9 if preceding else None
            w = row['diagnostic']['windows']
            dominant = [x['peaks'][0]['hz'] if x['peaks'] else None for x in w]
            row['dominantHzByWindow'] = dominant
            row['dominantFirstToLastCents'] = 1200 * math.log2(dominant[-1] / dominant[0]) if dominant and all(dominant) else None
        intervals = [(b-a)/1e9 for a, b in zip(cue_times, cue_times[1:])]
        epochs = (int(begins[0]['epochMillis']) / 1000, int(ends[0]['epochMillis']) / 1000)
        ui = []
        for line in lines:
            if ' I TomTuneTrace:' not in line:
                continue
            try:
                epoch = float(line.split()[0])
            except ValueError:
                continue
            if epochs[0] <= epoch <= epochs[1] and any(token in line for token in (' Confirmed ', ' SeriesReset ', ' UI ', ' Next ')):
                f = fields(line)
                f['event'] = next(token for token in ('Confirmed', 'SeriesReset', 'UI', 'Next') if f' {token} ' in line)
                ui.append(f)
        def selected(row, phase):
            return [(row[phase][c] or {}).get('hz', (row[phase][c] or {}).get('reason')) for c in configurations]
        blocks[block] = {'operator_declaration': operator['blocks'][block], 'beginMarkerGeneration': begins[0]['generation'],
                         'durationSeconds': (end-start)/1e9, 'cueIntervalsSeconds': intervals,
                         'cueIntervalMedianSeconds': statistics.median(intervals), 'configurations': configurations,
                         'sameNativeFramesAcrossConfigurations': all(len(set(r['native_detection_frames'].values())) == 1 for r in rows),
                         'sameNativeSelectionsAcrossConfigurations': all(len(set(selected(r, 'native'))) == 1 for r in rows),
                         'samePairedSelectionsAcrossConfigurations': all(len(set(selected(r, 'paired'))) == 1 for r in rows),
                         'strictCueWindowSeconds': [-.15, 1.20], 'strictCueMatchedEvents': sum(bool(r['strict_cue_matches']) for r in rows),
                         'strictCueUnmatchedEvents': sum(not r['strict_cue_matches'] for r in rows),
                         'uiEvents': ui, 'impacts': rows}
    outside = [r for r in base['impacts'] if (r['run'], r['generation'], r['impact']) not in used]
    return {'source': 'physical_microphone_metadata', 'playedCountsAreOperatorDeclared': True,
            'exactMissedOrDuplicatePhysicalStrokeCounts': 'not established independently', 'blocks': blocks,
            'outsideBothBlocks': outside, 'allDetectedGroups': len(base['impacts']),
            'allNextPresses': sum(e['event'] == 'Next' for e in base['ui_events']),
            'captureIssues': [fields(l) for l in lines if ' CaptureIssue ' in l]}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('trace', type=Path)
    p.add_argument('operator', type=Path)
    a = p.parse_args()
    print(json.dumps(analyze(a.trace.read_text(), json.loads(a.operator.read_text())), ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
