#!/usr/bin/env python3
"""Counted near-lug debug trial. Collects metadata only; does not record audio or change kits.
Run twice in one phone session: ten gentle strokes, then three firmer strokes on the SAME lug.
Each EXPECTED marker is an operator cue, not proof that a physical stroke was played.
"""
import argparse
import subprocess
import time
from pathlib import Path

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--adb', required=True)
p.add_argument('--serial', required=True)
p.add_argument('--output', required=True, type=Path)
p.add_argument('--count', type=int, default=10)
p.add_argument('--interval', type=float, default=2.0)
a = p.parse_args()
if not 1 <= a.count <= 50 or a.interval < 1:
    p.error('count 1..50; interval >= 1 second')
a.output.parent.mkdir(parents=True, exist_ok=True)
adb = [a.adb, '-s', a.serial]
run = str(time.time_ns())
with a.output.open('w') as log:
    capture = subprocess.Popen(adb + ['logcat', '-v', 'epoch', '-T', '1', 'TomTuneTrace:I', '*:S'], stdout=log)
    try:
        print('Same lug, microphone running, do not press Next. Starting cues in 3 seconds.', flush=True)
        time.sleep(3)
        first = time.monotonic()
        for i in range(a.count):
            time.sleep(max(0, first + i * a.interval - time.monotonic()))
            subprocess.run(adb + ['shell', 'log', '-p', 'i', '-t', 'TomTuneTrace',
                                  f'EXPECTED trial={run} ordinal={i+1} interval={a.interval}'], check=True)
            print(f'STRIKE {i+1}/{a.count}', flush=True)
        time.sleep(a.interval)
    finally:
        capture.terminate()
        capture.wait(timeout=5)
print(f'Metadata trace: {a.output}. Note actual stroke count and any late/omitted cues.')
