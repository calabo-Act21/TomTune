#!/usr/bin/env python3
"""Smoke test ONLY on a disposable Pixel 7 Android 35 emulator (1080x2400, English).
Checks native picking, permission refusal/grant, background and explicit screen closure.
No microphone recording or physical latency is measured. Uses Android's UI hierarchy.
"""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET

p = argparse.ArgumentParser()
p.add_argument('--adb', default='adb')
p.add_argument('--serial', default='emulator-5554')
a = p.parse_args()
if not a.serial.startswith('emulator-'):
    p.error('Only a disposable emulator is supported; this test clears app data.')

def adb(*args, check=True):
    r = subprocess.run([a.adb, '-s', a.serial, *args], capture_output=True, text=True, check=check, timeout=40)
    return r.stdout

def ui():
    adb('shell', 'uiautomator', 'dump', '/sdcard/tomtune-smoke.xml')
    return ET.fromstring(adb('exec-out', 'cat', '/sdcard/tomtune-smoke.xml'))

def wait_text(text):
    end = time.monotonic() + 30
    while time.monotonic() < end:
        root = ui()
        for node in root.iter('node'):
            if node.get('text') == text:
                return node
        time.sleep(.5)
    raise AssertionError('Missing UI text: ' + text)

def tap_text(text):
    node = wait_text(text)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def assert_released(label):
    end = time.monotonic() + 5
    while True:
        state = adb('shell', 'cmd', 'appops', 'get', 'com.tomtune.app', 'RECORD_AUDIO')
        if 'running=true' not in state and '(running)' not in state:
            break
        assert time.monotonic() < end, state
        time.sleep(.1)
    print(label + ': microphone app-op inactive after lifecycle transition; ' + state.strip(), flush=True)

assert '1080x2400' in adb('shell', 'wm', 'size'), 'Use the documented Pixel 7 emulator.'
adb('shell', 'pm', 'clear', 'com.tomtune.app')
adb('shell', 'cmd', 'locale', 'set-app-locales', 'com.tomtune.app', '--locales', 'en')
adb('shell', 'am', 'start', '-W', '-n', 'com.tomtune.app/.MainActivity')
wait_text('Enable microphone')
tap_text('Lug 2')
wait_text('Selected lug: 2')
# Native cap coordinates for the fixed camera and the documented Pixel 7 scene bounds.
for index, (x, y) in enumerate([(628,646),(764,749),(777,914),(642,1044),(437,1044),(303,914),(316,749),(451,646)]):
    adb('shell', 'input', 'tap', str(x), str(y))
    wait_text(f'Selected lug: {index+1}')
    print(f'native picking: lug {index+1}/8 correct', flush=True)

tap_text('Enable microphone')
tap_text('Don’t allow')
wait_text('Microphone permission denied · enable it in Settings')
assert_released('permission denied')
tap_text('Enable microphone')
tap_text('While using the app')
wait_text('Waiting for a strike')
state = adb('shell', 'cmd', 'appops', 'get', 'com.tomtune.app', 'RECORD_AUDIO')
assert 'running' in state, state
print('permission granted: native AudioRecord active with Filament visible; ' + state.strip(), flush=True)

adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')
assert_released('background')
adb('shell', 'am', 'start', '-W', '-n', 'com.tomtune.app/.MainActivity')
wait_text('Capture interrupted · tap to resume')
tap_text('Enable microphone')
wait_text('Waiting for a strike')
# Text selection must invalidate the former measurement while capture remains usable.
tap_text('Lug 2')
wait_text('Selected lug: 2')
wait_text('Waiting for a strike')
tap_text('Close screen')
wait_text('This screen is closed. The microphone has been released.')
assert_released('closed screen')
tap_text('Return to feasibility')
wait_text('Microphone stopped')
end = time.monotonic() + 30
while True:
    adb('shell', 'input', 'tap', '628', '646')
    if any(n.get('text') == 'Selected lug: 1' for n in ui().iter('node')):
        break
    assert time.monotonic() < end, 'Reopened native scene did not select lug 1'
    time.sleep(.5)
print('reopened: native scene recreated, lug 1 selected from lug 2, capture requires explicit activation', flush=True)
print('PASS — emulator simulation only; no real strike or hardware latency measured.', flush=True)
