#!/usr/bin/env python3
"""Read metadata only. Native detections and paired frequency choices are separate tables.
No count of physically played strokes is inferred from detections or cue markers.
"""
import argparse
import json
import math
import re
from collections import defaultdict
from pathlib import Path


def fields(line):
    return dict(re.findall(r'([A-Za-z0-9_]+)=([^\s]+)', line))


def summarize(text):
    impacts, selections, windows, modes = {}, {}, defaultdict(list), defaultdict(list)
    markers, ui, clocks = [], [], []
    for line in text.splitlines():
        f = fields(line)
        if 'CLOCK ' in line:
            clocks.append(f)
        if ' MARK ' in line:
            markers.append(f)
        if ' UI ' in line or any(x in line for x in [' SeriesReset ', ' Confirmed ', ' Next ', ' CaptureStart ', ' CaptureStop ']):
            ui.append(f | {'event': next((x for x in ['UI', 'SeriesReset', 'Confirmed', 'Next', 'CaptureStart', 'CaptureStop'] if f' {x} ' in line), '?')})
        if 'CMP ' not in line:
            continue
        capture = (f.get('run', 'injected'), f.get('generation', '0'))
        if 'CMP Impact ' in line:
            impacts[capture + (f['impact'],)] = f
        if f.get('phase') not in ('native', 'paired'):
            continue
        key = capture + (f['phase'], f['config'], f['impact'])
        if ' Selection ' in line:
            selections[key] = f
        if ' Window ' in line:
            windows[key].append(f)
        if ' Mode ' in line:
            modes[key].append(f)
    rows = []
    for key, impact in impacts.items():
        run, generation, ordinal = key
        row = {'run': run, 'generation': generation, 'impact': int(ordinal), 'frame': int(impact['frame']),
               'rate': int(impact['rate']), 'native_detection_frames': {}, 'paired': {}, 'native': {}}
        for config in ('lot0', 'current', 'band80_700'):
            row['native_detection_frames'][config] = None if impact[config] == 'missed' else int(impact[config])
            for phase in ('native', 'paired'):
                selection = selections.get((run, generation, phase, config, ordinal))
                row[phase][config] = selection
        rows.append(row)
    totals = {}
    for config in ('lot0', 'current', 'band80_700'):
        totals[config] = {'native_detected': sum(r['native_detection_frames'][config] is not None for r in rows),
                          'paired_accepted': sum('hz' in (r['paired'][config] or {}) for r in rows),
                          'paired_rejections': {}}
        reasons = totals[config]['paired_rejections']
        for row in rows:
            reason = (row['paired'][config] or {}).get('reason')
            if reason:
                reasons[reason] = reasons.get(reason, 0) + 1
    clock_by_capture = {(c.get('run', 'injected'), c.get('generation', '0')): c for c in clocks}
    cues = [m for m in markers if m.get('command') == 'cue']
    for row in rows:
        clock = clock_by_capture.get((row['run'], row['generation']))
        if not clock:
            row['cue_attribution'] = 'unavailable_without_audio_clock'
            continue
        onset_ns = int(clock['originMonotonicNanos']) + row['frame'] * 1e9 / row['rate']
        candidates = []
        for cue in cues:
            if (cue.get('run'), cue.get('generation')) != (row['run'], row['generation']):
                continue
            delta = (onset_ns - int(cue['monotonicNanos'])) / 1e9
            if -.15 <= delta <= 1.20:
                candidates.append((abs(delta), delta, cue))
        if candidates:
            _, delta, cue = min(candidates, key=lambda item: item[0])
            row.update(cue_block=cue['block'], cue_ordinal=int(cue['ordinal']), cue_offset_seconds=delta,
                       cue_attribution='in_cue_window_not_proof_of_played_stroke')
        else:
            row['cue_attribution'] = 'outside_cue_windows'
    return {'physical_stroke_count': 'requires operator annotations; never inferred', 'totals': totals,
            'impacts': rows, 'clocks': clocks, 'markers': markers, 'ui_events': ui,
            'cue_window_seconds': [-.15, 1.20],
            'window_rows': sum(map(len, windows.values())), 'mode_rows': sum(map(len, modes.values()))}



def injected_blocks(summary):
    """Only for the known 20-stroke synthetic fixture, with a series reset at the block boundary."""
    result = {}
    for block, lower, upper in [('low', 0, 20), ('high', 20, 40)]:
        rows = [r for r in summary['impacts'] if lower <= r['frame'] / r['rate'] < upper]
        configurations = {}
        for config in ('lot0', 'current', 'band80_700'):
            values, accepted, rejected, first = [], [], [], None
            for ordinal, row in enumerate(rows, 1):
                selection = row['native'][config] or {}
                if 'hz' not in selection:
                    if selection.get('reason'):
                        rejected.append({'stroke': ordinal, 'reason': selection['reason']})
                    continue
                hz = float(selection['hz'])
                accepted.append({'stroke': ordinal, 'hz': hz})
                values = (values + [hz])[-5:]
                spread = 1200 * math.log2(max(values)/min(values))
                if spread > 18 + 1e-8:
                    values = [hz]
                elif (len(values) >= 3 and spread <= 8 + 1e-8) or len(values) >= 5:
                    if first is None:
                        first = ordinal
            configurations[config] = {'native_detected': sum(r['native_detection_frames'][config] is not None for r in rows),
                                      'accepted': accepted, 'rejected': rejected, 'first_stable_stroke': first}
        result[block] = {'source': 'injected', 'known_strokes': 10, 'configurations': configurations}
    return result


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('trace', type=Path)
    p.add_argument('--injected-fixture', action='store_true')
    a = p.parse_args()
    summary = summarize(a.trace.read_text())
    if a.injected_fixture:
        summary['synthetic_blocks'] = injected_blocks(summary)
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
