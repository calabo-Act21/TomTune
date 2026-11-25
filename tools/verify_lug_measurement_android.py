#!/usr/bin/env python3
"""Emulator-only near-lug checks using in-memory injected PCM; no physical acoustic claim.
Seeds a diagnostic kit ONLY on emulator-5554, then leaves it unchanged after abandoning.
"""
import json
import time
import verify_lot23_emulator as ui


def trace():
    return ui.adb('logcat', '-d', '-v', 'epoch', 'TomTuneTrace:I', '*:S').decode()


def until(fragment, timeout=40):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        text = trace()
        if fragment in text:
            return text
        time.sleep(.25)
    raise AssertionError((fragment, text))


def next_enabled(root):
    nodes = [n for n in root.iter('node') if n.get('text') == 'Suivant']
    assert len(nodes) == 1, ui.strings(root)
    parents = {child: parent for parent in root.iter('node') for child in parent}
    return parents[nodes[0]].get('enabled') == 'true'


def bottom():
    return ui.dump() # live reading and Next are fixed; no scroll needed


def abandon():
    ui.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
    ui.click('Abandonner')


ui.adb('shell', 'am', 'force-stop', 'com.tomtune.app')
# run-as stdin avoids remote shell quoting of JSON.
fixture = {'schema': 1, 'activeId': 'diagnostic', 'kits': [{'id': 'diagnostic', 'name': 'PCM diagnostic', 'revision': 1,
    'drums': [{'id': 'tom', 'family': 'floor', 'order': 0, 'diameter': 16, 'lugs': 3, 'manualLugs': True},
              {'id': 'snare', 'family': 'snare', 'order': 0, 'diameter': 14, 'lugs': None, 'manualLugs': False},
              {'id': 'kick', 'family': 'kick', 'order': 0, 'diameter': 22, 'lugs': None, 'manualLugs': False}]}]}
import subprocess
ui.adb('shell', 'run-as', 'com.tomtune.app', 'mkdir', '-p', 'files')
subprocess.run([ui.a.adb, '-s', 'emulator-5554', 'shell', 'run-as', 'com.tomtune.app', 'tee', 'files/kits-v1.json'],
               input=json.dumps(fixture).encode(), stdout=subprocess.DEVNULL, check=True)
before = ui.snapshot()
ui.adb('logcat', '-c')
ui.adb('shell', 'am', 'start', '-W', '-n', 'com.tomtune.app/.MainActivity', '--ez', 'tomtune.injectedPcm', 'true',
       '--ez', 'tomtune.lugProbe', 'true')
ui.click('Floor tom 16″ · 3 vis par peau', True)
ui.click('Commencer l’accordage'); ui.click('Commencer par la peau de frappe')
until('Confirmed strike=3')
root = bottom()
assert next_enabled(root), ui.strings(root)
assert any('350' in s and 'Mesure stable' in s for s in ui.strings(root)), ui.strings(root)
ui.shot('350-stable-sans-avancement')
until('Confirmed strike=10')
assert ' Next ' not in trace()
print('PASS ten weak injected hits: same lug, stable on #3, still no Next at #10.', flush=True)
until('reason=AmbiguousModes')
root = bottom()
assert any('non concluante' in s.lower() for s in ui.strings(root)), ui.strings(root)
ui.shot('350-700-rejet-ambigu')
until('Estimate strike=12')
root = bottom()
assert not next_enabled(root), ui.strings(root)
assert any('Dernière mesure stable' in s and '350' in s for s in ui.strings(root)), ui.strings(root)
ui.shot('700-provisoire-ancienne350')
until('Confirmed strike=14')
root = bottom()
assert next_enabled(root), ui.strings(root)
assert any('700' in s and 'Mesure stable' in s for s in ui.strings(root)), ui.strings(root)
ui.shot('700-reconfirme-meme-vis')
ui.click('Suivant')
root = bottom()
assert not next_enabled(root), ui.strings(root)
log = trace()
assert log.count(' Next ') == 1
assert 'lug=2 CaptureStart' in log
(ui.a.output / 'probe-trace.log').write_text(log)
abandon(); assert ui.snapshot() == before
print('PASS ambiguous rejection, 700 provisional + previous350 + disabled Next, 700 stable; one Next advances one lug; abandon leaves kit unchanged.', flush=True)

# Check the same explicit-next interaction through both heads, without testing center/storage.
ui.adb('shell', 'am', 'force-stop', 'com.tomtune.app')
ui.adb('shell', 'am', 'start', '-W', '-n', 'com.tomtune.app/.MainActivity', '--ez', 'tomtune.injectedPcm', 'true')
ui.click('Floor tom 16″ · 3 vis par peau', True)
ui.click('Commencer l’accordage'); ui.click('Commencer par la peau de frappe')
ui.measure_head(3)
ui.click('Passer à la peau de résonance')
ui.measure_head(3)
root = ui.dump(); ui.shot('deux-peaux-conseils')
assert 'Vérifier le fût entier' in ui.strings(root)
abandon(); assert ui.snapshot() == before
(ui.a.output / 'two-heads-trace.log').write_text(trace())
print('PASS both heads: six stable drafts explicitly accepted by six Next presses; no center; fixture preserved.', flush=True)
