#!/usr/bin/env python3
"""Verify the 1.1A pilot's manifest and exact offline APK payload; keep lot 1 checks."""
import json
from pathlib import Path
import runpy
import struct
import zipfile
import argparse

runpy.run_path(str(Path(__file__).with_name('check_lot1_assets.py')), run_name='__main__')
parser = argparse.ArgumentParser()
parser.add_argument('--apk', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
assets = root / 'docs/visuels/lot1.1'
manifest = json.loads((assets / 'kit-r3-f2-zones.json').read_text())
assert manifest['key'] == 'r3-f2'
assert manifest['image'] == 'kit-r3-f2-alpha.png'
assert manifest['hitOrder'] == ['floor2', 'floor1', 'snare', 'rack1', 'rack2', 'rack3', 'kick']
assert set(manifest['hitOrder']) == set(manifest['regions'])
for name, polygon in manifest['regions'].items():
    assert len(polygon) >= 3, name
    assert all(len(p) == 2 and all(0 <= v <= 1 for v in p) for p in polygon), name
for name, expected in [('kit-r3-f2-alpha.png', (manifest['width'], manifest['height'])),
                       ('rack-12-6-alpha.png', (1254, 1254))]:
    data = (assets / name).read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    assert struct.unpack('>II', data[16:24]) == expected
    assert data[24:26] == bytes([8, 6]), '8-bit RGBA required'
if args.apk:
    with zipfile.ZipFile(args.apk) as apk:
        for path in assets.iterdir():
            if path.suffix in ('.png', '.json'):
                assert apk.read('assets/visuels/lot1.1/' + path.name) == path.read_bytes(), path
print('OK: 1.1A RGBA pilots, dimensions, 7 ordered polygons' + (', APK payload identical' if args.apk else ''))
