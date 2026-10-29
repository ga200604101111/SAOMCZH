from pathlib import Path
import unittest,json,os,re
W=Path(__file__).parents[1];R=W.parent;rules=Path(os.environ.get('SAO_ITEM_RULES',str(R/'SAO_TW_GitHub_Export/extensions/v2/rules-1.3.1.json')));dictionary=Path(os.environ.get('SAO_ITEM_DICT',str(R/'SAO_TW_Dynamic/zh_tw.json')));B=json.loads(rules.read_text('utf8'));D=json.loads(dictionary.read_text('utf8'));H={h['id']:h for h in B['hooks']}
def render(key,s):
 if key not in H:raise AssertionError('Missing verified item-detail hook '+key)
 h=H[key];v=h.get('exact',{}).get(s,D.get(h.get('section','names'),{}).get(s,s))
 for op in h.get('operations',[]):
  a,b=op['from'],op.get('to','')
  if op['op']=='replace_literal':v=v.replace(a,b)
  elif op['op']=='prefix' and v.startswith(a):v=b+v[len(a):]
  elif op['op']=='suffix' and v.endswith(a):v=v[:-len(a)]+b
 return v
class ItemDetails(unittest.TestCase):
 def test_detail_heading(self):self.assertEqual(render('items-economy:detail-visible-title','TRAINING PAVISE'),'訓練用大盾')
 def test_native_detail_name_before_case(self):self.assertEqual(render('items-economy:detail-info-name','Training Pavise'),'訓練用大盾')
 def test_acquisition(self):self.assertEqual(render('ui-text-layout:measured-cjk-wrap','Sold by Starting Merchant (1 Cols)'),'販售商人: 起始商人 (1 珂爾)')
 def test_acquisition_price_variants(self):
  for price in ['25','1200','1,250']:
   self.assertEqual(render('ui-text-layout:measured-cjk-wrap',f'Sold by Starting Merchant ({price} Cols)'),f'販售商人: 起始商人 ({price} 珂爾)')
 def test_item_batch_names(self):
  for source,target in [('Ancestral Root','遠古樹根'),('Magic Wood Shard','魔木碎片'),('Slime Core','史萊姆核心'),('Titan Bark','泰坦樹皮'),('Ninja Boots','忍者短靴')]:self.assertEqual(render('items-economy:item-plain',source),target)
 def test_gradient_formatted_french_aliases(self):
  for s,t in [('Écorce de Titan','泰坦樹皮'),('Bottes Furtives','戰術長靴'),('Amulette des Néphentès','豬籠草護符')]:self.assertEqual(D.get('items',{}).get(s),t)
 def test_quantity_suffix(self):
  for s,t in [('Healing Shard x2','治療碎片 x2'),('Mana Shard x3','魔力碎片 x3'),('Halloween Key x2','萬聖節鑰匙 x2')]:self.assertEqual(render('items-economy:item-plain',s),t)
 def test_effect_numbers(self):
  for s,t in [('Restores 40 mana','恢復 40 點魔力'),('Single use - cooldown of 1min','使用一次 - 冷卻時間 1 分鐘'),('Restores 150 HP, 150 mana, 150 stamina','恢復 150 點生命、150 點魔力、150 點耐力')]:self.assertEqual(render('items-economy:tooltip-display',s),t)
 def test_item_lore_sentence(self):self.assertEqual(render('ui-text-layout:measured-cjk-wrap','A rather rare loot from the Treant Warrior. Useful if you need to craft potions.'),'樹人戰士掉落的稀有材料，可用於製作藥水。')
 def test_skill_and_reward_captured_texts(self):
  for s,t in [('Stay out of combat for 3 seconds','脫離戰鬥 3 秒'),('Restores 60 HP at once. Single use.','立即恢復 60 點生命。使用一次。')]:self.assertEqual(D.get('lines',{}).get(s),t)
 def test_known_codex_fragments(self):self.assertEqual(D.get('lines',{}).get('A wild beast from the forests of the first floor.'),'來自第 1 層森林的野獸。')
 def test_no_general_chat_hook_or_filter(self):
  self.assertFalse(any(h['class'] in ('net/minecraft/class_338','net/minecraft/class_634') for h in B['hooks']))
  for s in ['ExamplePlayer: original message','AnotherPlayer: sample','null','old']:
   self.assertEqual(render('items-economy:detail-visible-title',s),s)
 def test_no_client_action_or_data_hook(self):
  h=H.get('items-economy:detail-visible-title',{});self.assertEqual(h.get('kind'),'field');self.assertEqual(h.get('method'),'render');self.assertEqual(h.get('member'),'upperName')
if __name__=='__main__':unittest.main()
