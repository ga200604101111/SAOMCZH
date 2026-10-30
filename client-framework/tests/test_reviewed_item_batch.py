from pathlib import Path
import json,re,unittest
R=Path(__file__).resolve().parents[2]
class ReviewedItemBatch(unittest.TestCase):
 def test_new_items_are_reviewed_once_and_reused(self):
  data=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  batch=json.loads((R/'client-framework/tests/reference/items-batch-20261030.json').read_text('utf8'))
  self.assertEqual(len(batch['entries']),319)
  for source,target in batch['entries'].items():
   with self.subTest(source=source):
    self.assertEqual(data['items'][source],target)
    self.assertTrue(all(source not in data[section] for section in ('names','lines','messages')))
    self.assertNotIn('：',target)
    self.assertEqual(re.findall(r'\d+(?:\.\d+)?',source),re.findall(r'\d+(?:\.\d+)?',target))
 def test_dynamic_durability_is_not_another_thousand_keys(self):
  data=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  for text in ['⛏ Durability: 123/448','You have: 17/15 ✔','Reward: +25 XP']:
   self.assertTrue(all(text not in data[s] for s in ('names','items','lines','messages')))
 def test_partial_or_unresolved_source_is_not_guessed(self):
  data=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  for text in ['{{TIMER}}',' ௗ  A horse mount not',' ௗ  Monture de cheval pas']:
   self.assertTrue(all(text not in data[s] for s in ('names','items','lines','messages')))
if __name__=='__main__':unittest.main()
