#!/usr/bin/env python3
"""Offline checks of the metadata parser and cues; no device access."""
import json
from pathlib import Path
from summarize_mode_comparison import summarize

root = Path(__file__).resolve().parents[1]
for rate in (8000, 48000):
    text = (root / f'docs/lot2-3/preuves/comparaison-vis/injected-{rate}.log').read_text()
    s = summarize(text)
    assert len(s['impacts']) == 20
    assert s['totals']['lot0']['native_detected'] == 17
    assert s['totals']['current']['paired_accepted'] == 16
    assert s['totals']['band80_700']['paired_accepted'] == 12
    assert not s['clocks'] and not s['markers']
    assert all('pcm' not in k.lower() for r in s['impacts'] for k in r)

# Clock is monotonic, anchored at AudioRecord frame zero. Cue is an operator instruction.
s = summarize('''LUG run=1 generation=2 MARK CLOCK rate=48000 originMonotonicNanos=1000000000
LUG run=1 generation=2 MARK command=cue block=light ordinal=1 monotonicNanos=2000000000
LUG run=1 generation=2 CMP Impact impact=1 frame=52800 rate=48000 lot0=missed current=52800 band80_700=52800
LUG run=1 generation=2 CMP phase=paired config=current impact=1 frame=52800 rate=48000 Selection rule=single_persistent hz=491.0
LUG run=1 generation=2 CMP Impact impact=2 frame=144000 rate=48000 lot0=144000 current=144000 band80_700=144000
LUG run=1 generation=2 UI readyForNext=false stableHz=null feedback=PROVISIONAL count=1
''')
assert s['impacts'][0]['cue_ordinal'] == 1
assert abs(s['impacts'][0]['cue_offset_seconds'] - .1) < 1e-8
assert s['impacts'][1]['cue_attribution'] == 'outside_cue_windows'
assert len(s['ui_events']) == 1
print('PASS metadata counts, paired/native distinction, monotonic PCM cue association, out-of-slot event; no physical inference.')
