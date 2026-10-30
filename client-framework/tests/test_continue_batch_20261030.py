from pathlib import Path
import json,unittest
ROOT=Path(__file__).resolve().parents[2]
class ContinueBatch(unittest.TestCase):
 def test_reviewed_clear_captures_are_added(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  expected={
   'names':{
    'Treant Warrior':'樹人戰士','Bandit Assassin':'盜匪刺客','Bandit Archer':'盜匪弓箭手',
    'Elite Treant':'精英樹人','Woodland Mage':'林地法師','Guardian Colossal':'巨型守衛',
    'Mine de Geldorak':'格爾多拉克礦坑','PvP Armor':'PvP 護甲','Off-hands':'副手裝備',
   },
   'lines':{
    'Explorer les tunnels':'探索隧道',
    'The Master Swordsman lets you try all five classes before you choose your path.':'劍術大師會讓你先體驗五種職業，再選擇自己的道路。',
    'See you soon.':'待會見。',
    'The Cliff':'懸崖',
   }
  }
  for sec,rows in expected.items():
   for k,v in rows.items():
    with self.subTest(sec=sec,k=k):self.assertEqual(d[sec].get(k),v)
 def test_cooldown_examples_are_not_fragmentary(self):
  d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  self.assertEqual(d['items'].get('Single use - cooldown of 1min'),'使用一次 - 冷卻時間: 1 分鐘')
  self.assertEqual(d['items'].get('3 uses - cooldown of 3min'),'可使用 3 次 - 冷卻時間: 3 分鐘')
if __name__=='__main__':unittest.main()
