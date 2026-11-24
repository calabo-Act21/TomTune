#!/usr/bin/env python3
"""Summarize debug metadata, never PCM. Frame-clock ticks do not prove screen presentation.
Usage: python3 tools/summarize_strikes.py trace.log [--reference-hz 185] [--played 20]
"""
import argparse
import json
import math
import re
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('trace', type=Path)
p.add_argument('--reference-hz', type=float)
p.add_argument('--played', type=int)
p.add_argument('--session', type=int)
p.add_argument('--run')
a = p.parse_args()
clocks, strikes = {}, {}
for line in a.trace.read_text().splitlines():
    match = re.search(r'\b(CLOCK|ANALYSIS|FRAME)\b', line)
    if not match:
        continue
    phase = match.group(1)
    values = dict(part.split('=', 1) for part in line[match.end():].split() if '=' in part)
    run = values.get('run', 'single-legacy-run')
    session = int(values['session'])
    if a.session is not None and session != a.session: continue
    if a.run is not None and run != a.run: continue
    if phase == 'CLOCK':
        clocks[(run, session)] = int(values['originNs'])
        continue
    strike = int(values['strike'])
    record = strikes.setdefault((run, session, strike), {'run': run, 'session': session, 'strike': strike})
    record[phase + '_' + values['kind']] = values

results = []
for (run, session, strike), record in strikes.items():
    onset = record.get('ANALYSIS_Detected')
    if not onset:
        continue
    origin = clocks.get((run, session))
    onset_ns = None if origin is None else origin + (float(onset['pcmSeconds']) - .005) * 1e9
    feedback = record.get('FRAME_Detected')
    estimate = record.get('ANALYSIS_Estimate')
    rejected = record.get('ANALYSIS_Rejected')
    hz_frame = record.get('FRAME_Estimate')
    hz = float(estimate['hz']) if estimate else None
    def delay(frame):
        return None if frame is None or onset_ns is None else round((int(frame['tNs']) - onset_ns) / 1e6, 2)
    results.append({
        'run': run, 'session': session, 'strike': strike, 'hz': hz,
        'rejected': rejected.get('reason') if rejected else None,
        'errorCents': round(1200 * math.log2(hz / a.reference_hz), 2) if hz and a.reference_hz else None,
        'rmsTriggerHopStartToDetectionFrameTickMs': delay(feedback),
        'rmsTriggerHopStartToHzFrameTickMs': delay(hz_frame),
        'detectionFrameClockTickObservedSeparately': feedback is not None,
    })
summary = {
    'scope': 'Device debug metadata: start of triggering RMS hop to Compose frame-clock tick after state composition. Neither physical attack nor screen presentation is measured; film both for end-to-end latency.',
    'playedByOperator': a.played,
    'detected': len(results),
    'estimates': sum(r['hz'] is not None for r in results),
    'rejected': sum(r['rejected'] is not None for r in results),
    'countDifference': None if a.played is None else len(results) - a.played,
    'warning': 'A count difference alone does not identify missed versus double detections; compare the filmed strike sequence.',
    'strikes': results,
}
print(json.dumps(summary, ensure_ascii=False, indent=2))
