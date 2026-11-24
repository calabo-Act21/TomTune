#!/usr/bin/env python3
"""Emulator-only permission/lifecycle checks. Normal microphone path, no acoustic claims."""
import verify_lot23_emulator as ui
import re,time

def resource_click(resource):
 root=ui.dump();n=next(n for n in root.iter('node')if n.get('resource-id','').endswith(resource))
 x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
 ui.adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));print('SYSTEM CLICK',resource,flush=True)
before=ui.snapshot()
ui.click('Rack tom 12″ · 6 vis par peau',True)
ui.click('Commencer l’accordage');ui.click('Commencer par la peau de frappe')
resource_click('permission_deny_button')
ui.click('Reprendre la mesure')
resource_click('permission_allow_foreground_only_button')
time.sleep(1)
ui.adb('shell','input','keyevent','KEYCODE_HOME');time.sleep(2)
ops=ui.adb('shell','cmd','appops','get','com.tomtune.app','RECORD_AUDIO').decode();print(ops,flush=True)
assert 'running' not in ops.lower(),ops
ui.adb('shell','am','start','-n','com.tomtune.app/.MainActivity')
ui.click('Reprendre la mesure')
root=ui.dump();ui.shot('micro-reprise')
# Back while recording opens the explicit discard dialog, with no writes.
ui.adb('shell','input','keyevent','KEYCODE_BACK');ui.click('Garder la session')
ui.click('Reprendre la mesure')
ui.adb('shell','input','keyevent','KEYCODE_BACK');ui.click('Abandonner')
root=ui.dump();assert any('Dernière mesure globale' in s for s in ui.strings(root))
assert ui.snapshot()==before
print('PASS: permission denied then granted; no recording in background; explicit resume same step; cancel discard then confirmed discard; snapshot unchanged.',flush=True)
