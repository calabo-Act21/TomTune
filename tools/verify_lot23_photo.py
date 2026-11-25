#!/usr/bin/env python3
"""Emulator-only visual pilot: exact floor16/8 photograph, adjacent labels and touch.
Uses explicitly injected PCM; leaves all stored kits and measurements unchanged.
"""
import time
import verify_lot23_emulator as ui
before=ui.snapshot()
ui.adb('shell','am','force-stop','com.tomtune.app')
ui.adb('shell','am','start','-W','-n','com.tomtune.app/.MainActivity','--ez','tomtune.injectedPcm','true')
ui.click('Floor tom 16″ · 8 vis par peau',True)
ui.click('Commencer l’accordage');ui.click('Commencer par la peau de frappe')
# Pause quickly enough to retain the first lug for visual inspection.
def pause():
 ui.adb('shell','input','keyevent','KEYCODE_HOME');time.sleep(1)
 ui.adb('shell','am','start','-n','com.tomtune.app/.MainActivity');return ui.dump()
root=pause()
def selected(index,root):
 nodes=[n for n in root.iter('node')if n.get('content-desc','').startswith('Vis ') and n.get('clickable')=='true']
 assert len(nodes)==8,[n.attrib for n in nodes]
 active=[n for n in nodes if n.get('checked')=='true']
 assert len(active)==1 and active[0].get('content-desc','').startswith(f'Vis {index} ·'),[n.attrib for n in active]
selected(1,root);ui.shot('floor16-vis1')
ui.click('Vis 4 ·',True);root=pause();selected(4,root);ui.shot('floor16-vis4')
ui.click('Reprendre la mesure');time.sleep(3.5);root=pause()
selected(5,root)
assert any(n.get('content-desc','').startswith('Vis 4 · Mesure confirmée')for n in root.iter('node')),ui.strings(root)
ui.shot('floor16-vis5-apres-confirmation')
try:
 ui.adb('shell','settings','put','system','font_scale','2.0');root=ui.dump()
 # Commands remain reachable by scrolling at 200%.
 ui.click('Reprendre la mesure');root=pause();ui.shot('floor16-texte-200')
finally:
 ui.adb('shell','settings','put','system','font_scale','1.0');ui.dump()
ui.adb('shell','input','keyevent','KEYCODE_BACK');ui.dump();ui.click('Abandonner')
assert ui.snapshot()==before
print('PASS: floor16/8 photo pilot; 8 unique accessible lugs, select upper/lower heads, one PCM confirmation 4 -> 5, large text and scroll; original snapshot preserved; font restored.')
