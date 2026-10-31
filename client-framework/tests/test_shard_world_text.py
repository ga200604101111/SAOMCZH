from pathlib import Path
import json,unittest,sys,hashlib
ROOT=Path(__file__).resolve().parents[2]
class ShardKeeperTextDisplay(unittest.TestCase):
 def test_exact_source_and_english_presentation_variants(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  self.assertEqual(d['names']['Shard Keeper'],'碎片管理員')
  self.assertEqual(d['names'].get('Gardien des Éclats'),'碎片管理員')
  self.assertEqual(d['lines'].get('Clic droit : vos éclats du jour'),'右鍵領取每日碎片。')
  self.assertEqual(d['lines'].get('Right-click: your shards of the day'),'右鍵領取每日碎片。')
  self.assertEqual(d['lines'].get('Right-click: your daily shards.'),'右鍵領取每日碎片。')
  self.assertEqual(d['lines'].get('Right-click: your daily shards'),'右鍵領取每日碎片。')
  self.assertNotIn('Gardien des Éclats \\ue001 Clic droit : vos éclats du jour',d['lines'],
    'The segment-aware renderer should handle this without duplicating merged PUA keys')
 def test_safe_pua_segmented_world_display_translation(self):
  s=(ROOT/'client-framework/src/sao/zh/engine/WorldTextSegments.java').read_text('utf8')
  self.assertIn("private static final char DISPLAY_SEPARATOR='\\uE001'",s)
  self.assertIn('SharedDictionary.lookup',s)
  self.assertIn('SemanticText.world',s)
  self.assertIn('MAX_SEGMENTS',s)
  self.assertNotIn('globalRegex',s)
  styled=(ROOT/'client-framework/src/sao/zh/engine/StyledNameProjection.java').read_text('utf8')
  self.assertIn("c=='\\uE001'",styled)
  self.assertIn('append(result,separator',styled)
 def test_no_generic_chat_and_title_surfaces(self):
  engine=(ROOT/'client-framework/src/sao/zh/engine/RenderTextPipeline.java').read_text('utf8')
  self.assertIn('WorldTextSegments.resolve',engine)
  self.assertIn('SemanticText.world(hook)',engine)
 def test_locale_integrity(self):
  p=ROOT/'languages/zh_tw/translations.json'
  v=json.loads((ROOT/'languages/zh_tw/zh-tw-version.json').read_text('utf8'))
  self.assertEqual(v['version'],'1.0.24')
  self.assertEqual(v['translation']['sha256'],hashlib.sha256(p.read_bytes()).hexdigest())
  self.assertEqual(v['translation']['size_bytes'],p.stat().st_size)
 def test_no_new_fabric_module_or_extra_hook(self):
  import time
  sys.path.insert(0,str(ROOT/'client-framework/tools'))
  from compose_localized import compose_bundle
  b=compose_bundle(ROOT,'zh_tw','1.15.0',119,int(time.time())+86400)
  self.assertEqual(len(b['hooks']),137)
if __name__=='__main__':unittest.main()
