#!/usr/bin/env python3
"""Single authorized Pixel session, metadata only. Never runs until invoked explicitly.
Tops are cues, not evidence of played strokes. Operator annotates each block separately.
Does not install an APK, press Next, advance a lug, or change a saved reference.
"""
import argparse
import json
import subprocess
import time
from pathlib import Path


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--adb', required=True)
    p.add_argument('--serial', required=True)
    p.add_argument('--output', required=True, type=Path)
    p.add_argument('--interval', type=float, default=3.0)
    a = p.parse_args()
    if a.interval < 2:
        p.error('interval must leave silence: >= 2 seconds')
    a.output.mkdir(parents=True, exist_ok=True)
    adb = [a.adb, '-s', a.serial]

    def marker(command, block, ordinal=0):
        subprocess.run(adb + ['shell', 'am', 'broadcast', '-a', 'com.tomtune.app.DEBUG_LUG',
                              '-p', 'com.tomtune.app', '--es', 'command', command,
                              '--es', 'block', block, '--ei', 'ordinal', str(ordinal)],
                       check=True, stdout=subprocess.DEVNULL)

    input('Open the SAME lug in the debug APK; leave microphone running and Next untouched. Enter when ready: ')
    observations = {'source': 'operator-declared', 'physical_trial': True, 'blocks': {}}
    with (a.output / 'metadata.log').open('w') as log:
        capture = subprocess.Popen(adb + ['logcat', '-v', 'epoch', '-T', '1', 'TomTuneTrace:I', '*:S'], stdout=log)
        try:
            for block, label in [('light', '10 coups légers'), ('firm', '10 coups un peu plus francs')]:
                input(f'{label}. Same physical lug, fixed damping, stick, point and mic. Enter to reset and start: ')
                marker('reset', block)
                # Capture reset must be observed in log after the session; never infer a reset from a cue.
                time.sleep(5)
                marker('begin', block)
                first = time.monotonic()
                for ordinal in range(1, 11):
                    time.sleep(max(0, first + (ordinal - 1) * a.interval - time.monotonic()))
                    marker('cue', block, ordinal)
                    print(f'TOP {ordinal}/10 · {block}', flush=True)
                time.sleep(5)
                marker('end', block)
                print('Stop playing. Cues alone do not establish actual strokes.', flush=True)
                played = input('Actually played cue ordinals, comma separated (e.g. 1,2,3,4,5,6,7,8,9,10), or unknown: ')
                notes = input('Any extra, late or uncertain strokes; fixed setup changes; independent tuner Hz if available: ')
                observations['blocks'][block] = {'played_ordinals_declaration': played, 'notes': notes,
                                                 'cue_count': 10, 'interval_seconds': a.interval}
                for item in played.split(','):
                    if item.strip().isdigit() and 1 <= int(item) <= 10:
                        marker('observed', block, int(item))
                (a.output / 'operator.json').write_text(json.dumps(observations, ensure_ascii=False, indent=2) + '\n')
        finally:
            capture.terminate()
            capture.wait(timeout=5)
    print('Saved metadata and operator declarations. No PCM. Do not count out-of-slot detections as played strokes.')


if __name__ == '__main__':
    main()
