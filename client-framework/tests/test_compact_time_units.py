from pathlib import Path
import json,os,zipfile,unittest,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'client-framework/tools'))
from build_glyph_contract_pack import compact_time_locale
class CompactTimePolicy(unittest.TestCase):
 def test_equipment_playtime_uses_english_original(self):
  lang={'saoui.equipment.playtime':'遊玩時間: ','friends.ago.minutes':'%s 分鐘','friends.ago.hours':'%s 小時',
        'bundle.timer_text':'剩餘時間: {days} 天 {hours} 小時 {minutes} 分鐘'}
  x=compact_time_locale('assets/triggersmod/lang/zh_tw.json',lang)
  self.assertEqual(x['saoui.equipment.playtime'],'Playtime:')
  self.assertEqual(x['friends.ago.minutes'],'%s min')
  self.assertEqual(x['friends.ago.hours'],'%s h')
  self.assertEqual(x['bundle.timer_text'],'剩餘時間: {days}d {hours}h {minutes}m')
  self.assertEqual(lang['saoui.equipment.playtime'],'遊玩時間: ')
  self.assertEqual(compact_time_locale('assets/triggersmod/lang/en_us.json',lang),lang)
 def test_native_equipment_duration_keeps_english_units(self):
  lang={'saoui.equipment.duration.hours':'%s 小時 %s','saoui.equipment.duration.minutes':'%s 分鐘'}
  v=compact_time_locale('assets/saoui/lang/zh_tw.json',lang)
  self.assertEqual(v['saoui.equipment.duration.hours'],'%sh %s')
  self.assertEqual(v['saoui.equipment.duration.minutes'],'%s min')
  self.assertEqual(compact_time_locale('assets/saoui/lang/zh_hk.json',lang),v)
  self.assertEqual(compact_time_locale('assets/saoui/lang/fr_fr.json',lang),lang)
 def test_unrelated_dialogue_and_plain_duration_unaffected(self):
  lang={'a':'我走了三小時。','b':'遊玩時間: 3 小時','saoui.equipment.playtime':'遊玩時間: '}
  v=compact_time_locale('assets/triggersmod/lang/zh_tw.json',lang)
  self.assertEqual(v['a'],lang['a'])
  self.assertEqual(v['b'],lang['b'])
  self.assertEqual(v['saoui.equipment.playtime'],'Playtime:')
 def test_short_time_translations_use_original_abbreviations(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  for sec in ('names','items','lines'):
   for raw in ('1min',' 1min','1min ',' 1min ','3min'):
    if raw in d[sec]:
     self.assertEqual(d[sec][raw],raw,(sec,raw))
   if 'Single use - cooldown of 1min' in d[sec]:
    self.assertEqual(d[sec]['Single use - cooldown of 1min'],'使用一次 - 冷卻時間: 1min')
   if '3 uses - cooldown of 3min' in d[sec]:
    self.assertEqual(d[sec]['3 uses - cooldown of 3min'],'可使用 3 次 - 冷卻時間: 3min')
  self.assertEqual(d['lines']['The pens are holding and my animals sleep. That is thanks to you.'],'圍欄守住了，我的牲畜也睡得安穩。這都多虧了你。')
 def test_transfer_duration_never_expands_short_units_or_english_words(self):
  import time
  from compose_localized import compose_bundle
  bundle=compose_bundle(ROOT,'zh_tw','1.13.0',117,int(time.time())+86400)
  h=next(h for h in bundle['hooks'] if h['id']=='worlds-locations:transfer-duration')
  self.assertEqual(h.get('operations',[]),[])
  for original in ('1h 30m','2hr 15min','45s','Class: assassin','Shepherd Ivo'):
   mapped=h.get('exact',{}).get(original,original)
   self.assertEqual(mapped,original)
 def test_localized_asset_in_candidate_pack(self):
  pack=os.environ.get('SAO_GLYPH_PACK')
  if not pack:self.skipTest('Candidate pack path not provided')
  with zipfile.ZipFile(pack) as z:
   for loc in ('zh_tw','zh_hk'):
    trigger=json.loads(z.read('assets/triggersmod/lang/'+loc+'.json'))
    saoui=json.loads(z.read('assets/saoui/lang/'+loc+'.json'))
    self.assertEqual(trigger['saoui.equipment.playtime'],'Playtime:')
    self.assertEqual(trigger['friends.ago.hours'],'%s h')
    self.assertEqual(trigger['friends.ago.minutes'],'%s min')
    self.assertEqual(trigger['bundle.timer_text'],'剩餘時間: {days}d {hours}h {minutes}m')
    self.assertEqual(saoui['saoui.equipment.duration.hours'],'%sh %s')
    self.assertEqual(saoui['saoui.equipment.duration.minutes'],'%s min')
if __name__=='__main__':unittest.main()
