from pathlib import Path
import json,sys,time,unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'client-framework/tools'))
from compose_localized import compose_bundle
from test_central_locale import engine

class DailyRewardQuantity(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  bundle=compose_bundle(ROOT,'zh_tw','1.7.0',109,int(time.time())+86400)
  cls.hooks={h['id']:h for h in bundle['hooks']}
  cls.dictionary=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
 def test_reward_name_has_no_duplicate_x2(self):
  for hook in ['items-economy:reward-name','items-economy:reward-cached-name']:
   for source in ['Healing Shard x2','治療碎片 x2']:
    for left,right in [('',''),(' ',''),('',' '),(' ',' ')]:
     with self.subTest(hook=hook,source=source,left=left,right=right):
      self.assertEqual(engine(self.hooks[hook],left+source+right,self.dictionary),left+'治療碎片'+right)
 def test_separate_count_line_is_unchanged(self):
  for key in ['items-economy:reward-name','items-economy:reward-cached-name']:
   for text in ['2','x2','×2','*2','x20','治療碎片']:
    with self.subTest(hook=key,text=text):self.assertEqual(engine(self.hooks[key],text,self.dictionary),text)
 def test_global_item_quantity_is_not_removed(self):
  self.assertEqual(self.dictionary['items']['Healing Shard x2'],'治療碎片 x2')
  self.assertEqual(self.dictionary['lines']['Healing Shard x2'],'治療碎片 x2')
  self.assertEqual(engine(self.hooks['items-economy:reward-name'],'Halloween Key x2',self.dictionary),'萬聖節鑰匙 x2')
 def test_only_reward_name_fields_are_changed(self):
  first=self.hooks['items-economy:reward-name'];cached=self.hooks['items-economy:reward-cached-name']
  self.assertEqual((first['class'],first['method'],first['kind']),('fr/sao/daily/DailyModel$Reward','name','return'))
  self.assertEqual((cached['class'],cached['method'],cached['member']),('fr/sao/daily/DailyScreen','drawSheet','sheetName'))
  self.assertTrue(all(h['method'] not in ('amount','give') for h in self.hooks.values() if '/daily/' in h['class']))

if __name__=='__main__':unittest.main()
