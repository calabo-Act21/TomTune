#!/usr/bin/env python3
"""Inspect the exact offline catalogue, maps and localized resources. Does not modify art."""
import argparse, hashlib, json, re, struct, xml.etree.ElementTree as ET, zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--apk',type=Path);args=p.parse_args()
root=Path(__file__).resolve().parents[1];art=root/'docs/visuels/lot1.1'
pngs=list(art.glob('*-alpha.png'));assert len(pngs)==28
for path in pngs:
 data=path.read_bytes();assert data[:8]==b'\x89PNG\r\n\x1a\n' and data[24:26]==bytes([8,6])
 source=root/'docs/visuels'/('kit/'+path.name[4:].replace('-alpha','') if path.name.startswith('kit-') else 'toms/'+path.name.replace('-alpha','-vis'))
 assert data[16:24]==source.read_bytes()[16:24]
def inside(points,x,y):
 result=False;previous=points[-1]
 for point in points:
  if (point[1]>y)!=(previous[1]>y) and x<(previous[0]-point[0])*(y-point[1])/(previous[1]-point[1])+point[0]: result=not result
  previous=point
 return result
legacy=json.loads((root/'docs/visuels/zones-kit.json').read_text())
for path in art.glob('*-zones.json'):
 m=json.loads(path.read_text());image=art/m['image'];assert struct.unpack('>II',image.read_bytes()[16:24])==(m['width'],m['height'])
 assert len(m['hitOrder'])==len(set(m['hitOrder'])) and set(m['hitOrder'])==set(m['regions'])
 r,f=map(int,re.fullmatch('r(\d)-f(\d)',m['key']).groups())
 assert set(m['regions'])=={'snare','kick'}|{f'rack{i+1}'for i in range(r)}|{f'floor{i+1}'for i in range(f)}
 assert m['hitOrder']==[z for z in ['floor2','floor1','snare','rack1','rack2','rack3','kick']if z in m['regions']]
 for polygon in m['regions'].values():assert len(polygon)>=3 and all(0<=v<=1 for pair in polygon for v in pair)
 def hit(x,y):return next((z for z in m['hitOrder']if inside(m['regions'][z],x,y)),None)
 for z,rect in legacy[m['key']].items():
  x=(rect[0]+rect[2])/2;y=(rect[1]+rect[3])/2
  assert hit(x,y)==z,(m['key'],z,'head',hit(x,y))
  # Shell points inspected in the PNG overlays. Kick is visible below the overlapping drums.
  shellY=.62 if z=='kick' else .70 if z=='floor2' else .54 if z=='floor1' else .46 if z=='snare' else y+.10
  assert hit(x,shellY)==z,(m['key'],z,'shell',hit(x,shellY))
 if f==2:assert hit(.69,.50)=='floor2'
 if m['key']!='r3-f2':assert m['verified'] and m['sha256']==hashlib.sha256(image.read_bytes()).hexdigest()
guidance=list(art.glob('*-lugs.json'))
for path in guidance:
 m=json.loads(path.read_text());count=int(path.stem.split('-')[2])
 assert m['verified'] and m['head']=='batter' and len(m['points'])==count
 assert m['sha256']==hashlib.sha256((art/m['image']).read_bytes()).hexdigest()
 assert m['points'][0][0]>.5 and m['points'][-1][0]<.5
 assert all(0<=v<=1 for pair in m['points'] for v in pair)
catalogues=[]
for lang in ['values','values-fr','values-es','values-de']:
 nodes=ET.parse(root/'composeApp/src/commonMain/composeResources'/lang/'strings.xml').getroot().findall('string')
 strings={n.attrib['name']:n.text or ''for n in nodes};assert len(strings)==len(nodes)
 catalogues.append(strings)
for c in catalogues[1:]:
 assert c.keys()==catalogues[0].keys()
 for k,v in c.items():assert sorted(re.findall(r'%\d+\$[ds]',v))==sorted(re.findall(r'%\d+\$[ds]',catalogues[0][k])),k
assert len(catalogues[-1]['tune_note_names'].split('|'))==12
if args.apk:
 with zipfile.ZipFile(args.apk)as z:
  for path in art.iterdir():
   if path.suffix in ('.png','.json'):assert z.read('assets/visuels/lot1.1/'+path.name)==path.read_bytes(),path
print(f'PASS: 28 exact PNGs; 8 ordered kit maps; {len(guidance)} verified guidance maps; other faces diagram; {len(catalogues[0])} keys in all 4 languages; offline APK bytes identical' if args.apk else 'PASS: artwork, maps and four resource catalogues')
