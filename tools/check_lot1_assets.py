#!/usr/bin/env python3
"""Check the supplied lot 1 catalogues and, optionally, their offline APK packaging."""
import argparse
import json
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[1]
assets = root / 'docs/visuels'
parser = argparse.ArgumentParser()
parser.add_argument('--apk', type=Path)
args = parser.parse_args()
zones = json.loads((assets / 'zones-kit.json').read_text())
focus = json.loads((assets / 'catalogue-focus.json').read_text())
assert set(zones) == {f'r{r}-f{f}' for r in range(4) for f in (1, 2)}
paths = ['catalogue-focus.json', 'zones-kit.json']
for key, entries in zones.items():
    racks, floors = int(key[1]), int(key[4])
    assert set(entries) == {'snare', 'kick'} | {f'rack{i+1}' for i in range(racks)} | {f'floor{i+1}' for i in range(floors)}
    path = f'kit/{key}.png'
    data = (assets / path).read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    assert struct.unpack('>II', data[16:24]) == (1536, 1024)
    paths.append(path)
    for name, (left, top, right, bottom) in entries.items():
        assert 0 <= left < right <= 1 and 0 <= top < bottom <= 1, (key, name)
    for i, (a, r) in enumerate(entries.items()):
        for b, s in list(entries.items())[i+1:]:
            assert not (max(r[0], s[0]) < min(r[2], s[2]) and max(r[1], s[1]) < min(r[3], s[3])), (key, a, b)
keys = set()
for item in focus:
    key = item['type'], item['diametrePouces'], item['visParPeau']
    assert key not in keys
    keys.add(key)
    assert len(item['points']) == item['visParPeau']
    # The runtime resolves handoff .jpg references to the supplied .png files.
    path = str(Path(item['fichier']).with_suffix('.png'))
    assert 'reference' not in path and (assets / path).is_file(), path
    paths.append(path)
resource_root = root / 'composeApp/src/commonMain/composeResources'
catalogues = []
for folder in ('values', 'values-fr', 'values-es', 'values-de'):
    catalogues.append({item.attrib['name']: item.text for item in ET.parse(resource_root / folder / 'strings.xml').getroot() if item.attrib['name'].startswith('kit_')})
assert all(set(catalogue) == set(catalogues[0]) for catalogue in catalogues)
assert all(all(catalogue.values()) for catalogue in catalogues)
if args.apk:
    with zipfile.ZipFile(args.apk) as apk:
        for path in paths:
            assert apk.read(f'assets/visuels/{path}') == (assets / path).read_bytes(), path
        assert not any('reference.png' in name for name in apk.namelist())
print(f'OK: {len(zones)} kit images, {len(focus)} exact focus variants, distinct zones, {len(catalogues[0])} strings in FR/EN/ES/DE' + (', APK assets identical to sources' if args.apk else ''))
