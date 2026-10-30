from pathlib import Path
import json,sys,time,unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'client-framework/tools'))
from compose_localized import compose_bundle
class CrossSurfaceSources(unittest.TestCase):
 def test_flattened_hud_text_is_projected_before_measure(self):
  b=compose_bundle(ROOT,'zh_tw','1.10.0',114,int(time.time())+86400)
  h={x['id']:x for x in b['hooks']}
  info=h.get('quests-objectives:compass-flat-info')
  self.assertIsNotNone(info,'Translate the flattened source before splitInfo stores text and widths')
  self.assertEqual((info['method'],info['kind'],info['member'],info['sites']),('splitInfo','call','flat',2))
  city=h['quests-objectives:compass-flat-city']
  self.assertEqual((city['method'],city['kind'],city['member']),('drawTitle','call','of'))
 def test_hud_title_is_scoped_not_global_messages_lookup(self):
  b=compose_bundle(ROOT,'zh_tw','1.10.0',114,int(time.time())+86400)
  h={x['id']:x for x in b['hooks']}
  for key in ('hud-location-title','hud-location-subtitle'):
   t=h.get('worlds-locations:'+key);self.assertIsNotNone(t)
   self.assertEqual(t['class'],'net/minecraft/class_329');self.assertEqual(t['section'],'names');self.assertNotIn('capture',t)
  self.assertLessEqual(len(h),128)
 def test_single_canonical_new_translations(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  expected={'Shard Keeper':'碎片管理員','Copper Katana':'銅製武士刀','Right-click: your daily shards.':'右鍵領取每日碎片。'}
  for source,target in expected.items():
   matches=[sec for sec in ('names','items','lines','messages') if source in d[sec]]
   self.assertEqual(len(matches),1,source)
   self.assertEqual(d[matches[0]][source],target)
  self.assertEqual(d['names']['Hanaka'],'哈納卡')
  self.assertIn('Forged from copper,\nits blade is more solid and\nsharper than the others',d['items'])
if __name__=='__main__':unittest.main()
