#!/usr/bin/env python3
"""Explicit emulator-only UI recipe. Uses the debug PCM intent, never the microphone.
Does not reset kits. The selected tom receives the deliberately finished test measurement.
"""
import argparse, json, re, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
a.output.mkdir(parents=True,exist_ok=True)
def adb(*args):return subprocess.check_output([a.adb,'-s','emulator-5554',*args])
def dump():
 adb('shell','uiautomator','dump','/sdcard/lot23-ui.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/lot23-ui.xml'))
def strings(root):return [n.get('text') or n.get('content-desc') for n in root.iter('node') if n.get('text') or n.get('content-desc')]
def click(label, desc=False):
 for attempt in range(9):
  root=dump()
  matches=[n for n in root.iter('node') if (n.get('content-desc','').startswith(label) if desc else n.get('text','')==label)]
  if matches:
   n=matches[0];x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
   adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));print('CLICK',label,flush=True);return
  adb('shell','input','swipe','800','2000','800','850','350')
 raise AssertionError((label,strings(root)))
def shot(name):
 (a.output/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
 (a.output/(name+'.xml')).write_bytes(adb('shell','cat','/sdcard/lot23-ui.xml'))
def wait_text(label, timeout=70):
 deadline=time.monotonic()+timeout
 while time.monotonic()<deadline:
  root=dump()
  if any(n.get('text')==label for n in root.iter('node')):return root
  adb('shell','input','swipe','800','2000','800','850','300')
  time.sleep(2)
 raise AssertionError((label,strings(root)))
def measure_head(count=6):
 for _ in range(count):
  deadline=time.monotonic()+15
  while time.monotonic()<deadline:
   root=dump()
   parents={c:p for p in root.iter('node')for c in p}
   buttons=[n for n in root.iter('node')if n.get('text')=='Suivant' and parents[n].get('enabled')=='true']
   if buttons:break
   adb('shell','input','swipe','800','2000','800','850','250');time.sleep(.5)
  else:raise AssertionError(('Next never enabled',strings(root)))
  click('Suivant')
 return wait_text('Vérifier cette peau')
def snapshot():return json.loads(adb('shell','run-as','com.tomtune.app','cat','files/kits-v1.json'))
def run_recipe():
 adb('shell','am','force-stop','com.tomtune.app')
 adb('shell','am','start','-W','-n','com.tomtune.app/.MainActivity','--ez','tomtune.injectedPcm','true')
 time.sleep(1)
 before=snapshot();(a.output/'kits-before.json').write_text(json.dumps(before,ensure_ascii=False,indent=2))
 root=dump();assert 'PCM injecté · microphone désactivé' in strings(root)
 click('Rack tom 12″ · 6 vis par peau',True)
 click('Commencer l’accordage');shot('cible')
 click('Commencer par la peau de frappe')
 time.sleep(1)
 adb('shell','input','keyevent','KEYCODE_HOME');time.sleep(2)
 adb('shell','am','start','-n','com.tomtune.app/.MainActivity')
 click('Reprendre la mesure')
 measure_head();shot('frappe-conseils')
 click('Vérifier cette peau');measure_head()
 click('Passer à la peau de résonance');measure_head();shot('resonance-conseils')
 click('Vérifier le fût entier');wait_text('Terminer');shot('centre-avant-correction')
 click('Refaire une passe de correction');measure_head()
 click('Passer à la peau de résonance');measure_head()
 click('Vérifier le fût entier');wait_text('Terminer');shot('centre-final')
 click('Terminer');time.sleep(.5)
 root=dump();assert any('Dernière mesure globale' in s for s in strings(root)),strings(root)
 shot('fiche-finale')
 after=snapshot();(a.output/'kits-after.json').write_text(json.dumps(after,ensure_ascii=False,indent=2))
 old=next(k for k in before['kits'] if k['id']==before['activeId']);new=next(k for k in after['kits'] if k['id']==after['activeId'])
 assert len(before['kits'])==len(after['kits']) and before['activeId']==after['activeId']
 changed=[(x,y)for x,y in zip(old['drums'],new['drums'])if x!=y];assert len(changed)==1,changed
 x,y=changed[0];assert abs(y['lastMeasurement']['hz']-120)<.25;assert x.get('reference')==y.get('reference')
 assert {k:v for k,v in x.items()if k!='lastMeasurement'}=={k:v for k,v in y.items()if k!='lastMeasurement'}
 adb('shell','am','force-stop','com.tomtune.app');adb('shell','am','start','-W','-n','com.tomtune.app/.MainActivity')
 assert snapshot()==after
 print('PASS: injected PCM UI; batter repeat, resonant, center, correction through both heads, finish same tom; background/resume; all other drums/kits preserved; normal offline relaunch.',flush=True)

if __name__=='__main__': run_recipe()
