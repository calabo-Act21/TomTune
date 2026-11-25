#!/usr/bin/env python3
"""Join explicit counted trial cues to debug metadata; never infer physical strokes from PCM.
MissedExpected = no detection in a cue window, requiring operator confirmation of played strokes.
Without EXPECTED markers, reports observed events only, never invented misses.
"""
import argparse
import json
import re
from pathlib import Path


def summarize(lines):
    records = []
    for line in lines:
        match = re.search(r'TomTuneTrace\s*:\s*(LUG|EXPECTED)\s+(.*)', line)
        if not match:
            continue
        fields = dict(re.findall(r'(\w+)=([^\s]+)', match[2]))
        stamp = re.match(r'\s*(\d+\.\d+)', line)
        fields['time'] = float(stamp[1]) if stamp else None
        fields['kind'] = 'EXPECTED' if match[1] == 'EXPECTED' else next(
            (part for part in match[2].split() if '=' not in part), 'Unknown')
        records.append(fields)
    detections = [r for r in records if r['kind'] == 'Detected']
    trials = []
    for cue in (r for r in records if r['kind'] == 'EXPECTED'):
        width = float(cue.get('interval', 2))
        # Non-overlapping cue slots, half an interval on each side.
        hits = [r for r in detections if cue['time'] is not None and r['time'] is not None
                and cue['time'] - width / 2 <= r['time'] < cue['time'] + width / 2]
        if len({(r.get('run'), r.get('session'), r.get('generation'), r.get('lug')) for r in hits}) > 1:
            status = 'ContextChanged'
        else:
            status = 'MissedExpected' if not hits else 'Detected' if len(hits) == 1 else 'MultipleDetections'
        keys = {(r.get('run'), r.get('generation'), r.get('strike')) for r in hits}
        results = [r for r in records if r['kind'] in ('Estimate', 'Rejected', 'Confirmed') and
                   (r.get('run'), r.get('generation'), r.get('strike')) in keys]
        trials.append({'trial': cue.get('trial'), 'ordinal': int(cue['ordinal']), 'status': status,
                       'detections': len(hits), 'results': results})
    return {'scope': 'Debug metadata only. Cue windows require operator timing/count confirmation; no acoustic accuracy inferred.',
            'detected': len(detections), 'rejected': [r for r in records if r['kind'] == 'Rejected'],
            'estimates': [r for r in records if r['kind'] == 'Estimate'],
            'resets': [r for r in records if r['kind'] == 'SeriesReset'],
            'confirmed': [r for r in records if r['kind'] == 'Confirmed'],
            'advances': [r for r in records if r['kind'] == 'Next'], 'countedTrials': trials}


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('trace', type=Path)
    a = p.parse_args()
    print(json.dumps(summarize(a.trace.read_text().splitlines()), ensure_ascii=False, indent=2))
