from pathlib import Path
import json,re,sys,time,unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'client-framework/tools'))
from compose_localized import compose_bundle
class Typography(unittest.TestCase):
 def test_chinese_field_colons_have_one_following_space(self):
  data=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  bad=[]
  for section in ('names','items','lines','messages'):
   for key,value in data[section].items():
    if re.search(r'[\u3400-\u9fff][ \t\u00a0\u3000]*[:：](?! (?![ \t\u00a0\u3000]))',value) or re.search(r'[\u3400-\u9fff][ \t\u00a0\u3000]+:',value):bad.append((section,key,value))
  self.assertEqual(bad,[],str(bad[:5]))
 def test_discovery_uses_shared_names_before_native_width(self):
  b=compose_bundle(ROOT,'zh_tw','1.11.0',115,int(time.time())+86400)
  h=next((x for x in b['hooks'] if x['id']=='worlds-locations:discovery-title'),None)
  self.assertIsNotNone(h,'Discovery banner is not the compass or vanilla title setter')
  self.assertEqual((h['class'],h['method'],h['kind'],h['section'],h['member'],h['sites']),('fr/sao/saoui/discovery/DiscoveryOverlayRenderer','onHudRender','field','names','titleText',1))
  self.assertNotIn('capture',h)
 def test_original_match_keys_and_level_are_intact(self):
  data=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  self.assertEqual(data['items']['Attack Damage :'],'攻擊傷害: ')
  self.assertEqual(data['names']['HANAKA'],'哈納卡')
if __name__=='__main__':unittest.main()
