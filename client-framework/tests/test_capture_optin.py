from pathlib import Path
import json,unittest,sys,time
W=Path(__file__).resolve().parents[1]
if not (W/'extensions/modules').is_dir():W=W.parent
sys.path.insert(0,str(W/'tools' if (W/'tools/compose_localized.py').is_file() else W/'client-framework/tools'))
from compose_modules import checked_hook,ModuleError
from compose_localized import compose_bundle
class CaptureOptIn(unittest.TestCase):
 def test_only_explicit_domain_display_origins(self):
  b=compose_bundle(W,'zh_tw','1.6.0',108,int(time.time())+86400*100)
  c=[h for h in b['hooks'] if h.get('capture')]
  self.assertGreaterEqual(len(c),20)
  for h in c:
   self.assertIn(h['capture'],('quest','codex','skill','item','world','system'))
   self.assertIn(h['locale'],('zh_tw',))
   self.assertNotIn(h['class'],('net/minecraft/class_338','net/minecraft/class_634'))
   self.assertNotIn(h['class'],('fr/sao/pass/screen/PassCanvas','fr/sao/font/TextUtil'))
  named={h['id']:h['capture'] for h in c}
  self.assertEqual(named['quests-objectives:quest-row-title'],'quest')
  self.assertEqual(named['codex-creatures:codex-display-lore'],'codex')
  self.assertEqual(named['skills-progression:skilltree-lore-read'],'skill')
  self.assertEqual(named['items-economy:detail-info-name'],'item')
  self.assertEqual(named['worlds-locations:boss-plain'],'world')
 def test_no_capture_for_chat_or_guild_user_data(self):
  b=compose_bundle(W,'zh_tw','1.6.0',108,int(time.time())+86400*100)
  self.assertTrue(all(not h.get('capture') for h in b['hooks'] if h['id'].startswith('guild-social:')))
  self.assertTrue(all(not h.get('capture') for h in b['hooks'] if h['id'].startswith('ui-text-layout:')))
 def test_wrong_owner_rejected_even_if_module_signed(self):
  bad={'id':'mock','class':'fr/sao/pass/screen/PassCanvas','method':'text',
       'descriptor':'(Ljava/lang/String;)V','kind':'argument','argument':0,
       'section':'lines','capture':'quest'}
  with self.assertRaises(ModuleError):checked_hook(bad,'quests-objectives',{})
  bad['class']='net/minecraft/class_338'
  with self.assertRaises(ModuleError):checked_hook(bad,'quests-objectives',{})
  bad['class']='fr/sao/codex/CodexScreen'
  with self.assertRaises(ModuleError):checked_hook(bad,'codex-creatures',{})
if __name__=='__main__':unittest.main()
