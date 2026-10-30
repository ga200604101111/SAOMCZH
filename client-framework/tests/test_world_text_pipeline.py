from pathlib import Path
import sys,json,unittest,time
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'client-framework/tools'))
from compose_modules import checked_hook,ModuleError
from compose_localized import compose_bundle
class WorldTextPipeline(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.module=json.loads((ROOT/'extensions/modules/worlds-locations/module.json').read_text('utf8'))
  cls.bundle=compose_bundle(ROOT,'zh_tw','1.7.0',109,int(time.time())+86400*70)
  cls.hooks={h['id']:h for h in cls.bundle['hooks']}
 def test_precise_native_before_measure(self):
  h=self.hooks['worlds-locations:text-display-before-native-wrap']
  self.assertEqual(h['class'],'net/minecraft/class_8138$class_8141')
  self.assertEqual(h['method'],'method_49057')
  self.assertEqual(h['kind'],'argument');self.assertEqual(h['argument'],0)
  self.assertEqual(h['section'],'lines')
  self.assertNotIn('capture',h)
 def test_saojob_hologram_preformat_and_styled_return(self):
  for id,kind in [('saojob-hologram-preformat','argument'),('saojob-hologram-styled-text','return')]:
   h=self.hooks['worlds-locations:'+id]
   self.assertEqual(h['class'],'com/eighteen/saojob/hologram/HologramRenderer')
   self.assertEqual(h['method'],'parseLegacySectionFormatting')
   self.assertEqual(h['descriptor'],'(Ljava/lang/String;)Lnet/minecraft/class_2561;')
   self.assertEqual(h['kind'],kind)
   self.assertNotIn('capture',h)
 def test_single_allowlisted_epoch_cache_invalidation(self):
  h=self.hooks['worlds-locations:text-display-cache-refresh']
  self.assertEqual(h['class'],'net/minecraft/class_8113$class_8123')
  self.assertEqual(h['method'],'method_48905')
  self.assertEqual(h['kind'],'refresh')
  self.assertEqual(h['cache_fields'],[{'name':'field_42442','descriptor':'Lnet/minecraft/class_8113$class_8123$class_8125;','action':'reset_display_lines'}])
  bad=dict(h);bad['class']='net/minecraft/class_338'
  with self.assertRaises(ModuleError):checked_hook({k:v for k,v in bad.items() if k not in ('locale','exact','operations')},'worlds-locations',{})
  bad=dict(h);bad['cache_fields']=[{'name':'field_53588','descriptor':'Lnet/minecraft/class_8113$class_8123$class_8125;','action':'reset_display_lines'}]
  with self.assertRaises(ModuleError):checked_hook({k:v for k,v in bad.items() if k not in ('locale','exact','operations')},'worlds-locations',{})
 def test_translation_is_full_string_and_not_player_names(self):
  data=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  expected={'Jump and Run (Easy)':'跳躍跑酷（簡單）',
            '(Respect if you finish it)':'（成功通關者值得敬佩）',
            'Jump and Run (Easy)\n(Respect if you finish it)':'跳躍跑酷（簡單）\n（成功通關者值得敬佩）'}
  for src,target in expected.items():
   self.assertEqual(data['lines'][src],target)
   self.assertNotIn(src,data['names'])
   self.assertNotIn(src,data['messages'])
  self.assertEqual(len(self.hooks),128)
  self.assertEqual(self.bundle['translation_version'],'1.0.17')
 def test_armorstand_only_render_state_not_entity_or_player(self):
  hook=self.hooks['worlds-locations:armorstand-nameplate']
  self.assertEqual(hook['class'],'net/minecraft/class_877')
  self.assertEqual(hook['method'],'method_62375')
  self.assertEqual(hook['kind'],'armorstand_nameplate')
  self.assertEqual(hook['section'],'lines')
  self.assertNotIn('capture',hook)
  for override in [{'class':'net/minecraft/class_897'},{'method':'method_3936'},{'section':'names'}]:
   broken=dict(hook,**override)
   with self.assertRaises(ModuleError):
    checked_hook({k:v for k,v in broken.items() if k not in ('locale','exact','operations')},'worlds-locations',{})
 def test_no_global_chat_or_renderer_capture(self):
  h=[h for h in self.hooks.values() if h['id'].startswith('worlds-locations:') and ('hologram' in h['id'] or 'text-display' in h['id'])]
  self.assertEqual(len(h),4)  # Two TextDisplay and two SAOJob hooks; ArmorStand is independently scoped
  self.assertTrue(all(hook['locale']=='zh_tw' and hook['section']=='lines' for hook in h))
  self.assertTrue(all(not hook.get('capture') for hook in h))
  self.assertNotIn('net/minecraft/class_338',[h['class'] for h in h])
if __name__=='__main__':unittest.main()
