from pathlib import Path
import json,unittest
ROOT=Path(__file__).resolve().parents[2]
class ScreenshotMessages(unittest.TestCase):
 def test_screenshot_fixed_messages(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  expected={
   'Fuel ready! Right-click to start heating.':'燃料已備妥！右鍵開始加熱。',
   'Heating started! Right-click again to stop at the right temperature.':'開始加熱！在適當溫度時再次右鍵停止。',
   "Hey! Sorry, but you can't change that here.":'嘿！抱歉，你不能在這裡更改。',
   'Cooking started! Right-click to retrieve at the optimal moment.':'開始加熱處理！在最佳時機右鍵取出。',
   'Quenching in progress...':'淬火進行中……',
   'Process complete! Right-click to retrieve the item.':'處理完成！右鍵取出物品。',
  }
  for k,v in expected.items():
   with self.subTest(k=k):self.assertEqual(d['messages'].get(k),v)
 def test_world_names_have_explicit_provenance_keys(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  self.assertEqual(d['names'].get('Eugeo'),'尤吉歐')
  self.assertEqual(d['names'].get('Cardinal Relay'),'卡迪納爾中繼裝置')
  self.assertNotIn('Player_One',d['names'])
 def test_item_translations_reused_not_copied_to_messages(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  for k in ('Coal Ore','Creuset de Forge','Lingot de Fer Chaud'):
   self.assertIn(k,d['items']);self.assertNotIn(k,d['messages'])
 def test_formatted_numeric_messages_are_not_hardcoded(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  for k in ('Heating stopped at 108°C. Right-click with the item to insert it.','You added 4x Lingot de Fer Chaud','Furnace — temperature 100 pts | time 100 pts | precision 100%'):
   self.assertNotIn(k,d['messages'])
 def test_colons_keep_ascii_and_level_unchanged(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  self.assertFalse(any('：' in v for section in ('names','items','lines','messages') for v in d[section].values()))
if __name__=='__main__':unittest.main()
