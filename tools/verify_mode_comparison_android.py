#!/usr/bin/env python3
"""Emulator only, injected PCM. Checks Android integration, debug reset and no automatic Next.
Reuses an existing diagnostic kit; never addresses a physical Android terminal.
"""
import time
import verify_lot23_emulator as ui
from summarize_mode_comparison import summarize


def trace():
    return ui.adb('logcat', '-d', '-v', 'epoch', 'TomTuneTrace:I', '*:S').decode()


def marker(command):
    ui.adb('shell', 'am', 'broadcast', '-a', 'com.tomtune.app.DEBUG_LUG', '-p', 'com.tomtune.app',
           '--es', 'command', command, '--es', 'block', 'injected')


before = ui.snapshot()
# Coherent injected mode: confirm, manual Next, then reset a confirmed draft on lug 2.
ui.adb('shell', 'am', 'force-stop', 'com.tomtune.app')
ui.adb('logcat', '-c')
ui.adb('shell', 'am', 'start', '-W', '-n', 'com.tomtune.app/.MainActivity', '--ez', 'tomtune.injectedPcm', 'true')
ui.click('Floor tom 16″ · 3 vis par peau', True)
ui.click('Commencer l’accordage'); ui.click('Commencer par la peau de frappe')
for lug in (1, 2):
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        if any(f'lug={lug} Confirmed ' in line for line in trace().splitlines()):
            break
        time.sleep(.2)
    else:
        raise AssertionError('coherent injection did not confirm')
    root = ui.dump()
    parents = {c: p for p in root.iter('node') for c in p}
    assert any(n.get('text') == 'Suivant' and parents[n].get('enabled') == 'true' for n in root.iter('node'))
    if lug == 1:
        assert ' Next ' not in trace()
        ui.click('Suivant')
marker('reset')
root = ui.dump()
parents = {c: p for p in root.iter('node') for c in p}
assert any(n.get('text') == 'Suivant' and parents[n].get('enabled') == 'false' for n in root.iter('node'))
assert trace().count(' Next ') == 1
assert 'DebugReset reason=operator' in trace()
(ui.a.output / 'coherent-next-reset.log').write_text(trace())
(ui.a.output / 'coherent-reset-state.xml').write_bytes(ui.adb('shell', 'cat', '/sdcard/lot23-ui.xml'))
ui.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK'); ui.click('Abandonner')
assert ui.snapshot() == before
print('PASS coherent injected mode: confirmation enables Next, one press advances one lug, reset clears stable draft and disables Next.', flush=True)
ui.adb('shell', 'am', 'force-stop', 'com.tomtune.app')
ui.adb('logcat', '-c')
ui.adb('shell', 'am', 'start', '-W', '-n', 'com.tomtune.app/.MainActivity',
       '--ez', 'tomtune.injectedPcm', 'true', '--ez', 'tomtune.modeProbe', 'true')
ui.click('Floor tom 16″ · 3 vis par peau', True)
ui.click('Commencer l’accordage'); ui.click('Commencer par la peau de frappe')
deadline = time.monotonic() + 65
while time.monotonic() < deadline:
    text = trace()
    if any('phase=paired config=band80_700 impact=20 ' in line and ' Selection ' in line for line in text.splitlines()):
        break
    time.sleep(.3)
else:
    raise AssertionError('20 paired impacts not completed')
summary = summarize(text)
assert len(summary['impacts']) == 20, summary['totals']
assert summary['totals']['current']['native_detected'] == 20
assert summary['totals']['lot0']['native_detected'] == 17
assert summary['totals']['current']['paired_accepted'] == 16
assert summary['totals']['band80_700']['paired_accepted'] == 12
assert ' Next ' not in text
assert 'Overrun' not in text
assert 'impact=null' not in text
assert 'UI readyForNext=false' in text
marker('cue')
marker('reset')
time.sleep(.4)
text = trace()
assert 'DebugReset reason=operator' in text
assert 'MARK command=reset block=injected' in text
assert text.count(' CaptureStart ') == 2
root = ui.dump()
parents = {c: p for p in root.iter('node') for c in p}
buttons = [n for n in root.iter('node') if n.get('text') == 'Suivant']
assert len(buttons) == 1 and parents[buttons[0]].get('enabled') == 'false'
(ui.a.output / 'comparison-trace.log').write_text(text)
(ui.a.output / 'reset-state.xml').write_bytes(ui.adb('shell', 'cat', '/sdcard/lot23-ui.xml'))
ui.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK'); ui.click('Abandonner')
assert ui.snapshot() == before
print('PASS Android injected 491/699/801: 20 same-PCM impacts, lot0=17/current=20/band=20 native detections; current=16/band=12 paired accepts; no Overrun/no automatic Next; debug reset starts new capture, Next disabled; abandon preserves kit.', flush=True)
