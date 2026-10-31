from pathlib import Path
import json,hashlib,unittest,os
ROOT=Path(__file__).resolve().parents[2]
class CrossSurface20261031(unittest.TestCase):
 def setUp(self):
  self.d=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
 def test_container_and_actionbar_hooks_are_allowlisted(self):
  doc=json.loads((ROOT/'extensions/modules/named-container/module.json').read_text('utf8'))
  self.assertEqual(doc['id'],'named-container')
  self.assertEqual(len(doc['hooks']),1)
  h=doc['hooks'][0]
  self.assertEqual((h['class'],h['method'],h['descriptor'],h['kind'],h['section'],h['member'],h['sites']),
    ('net/minecraft/class_465','method_2388','(Lnet/minecraft/class_332;II)V','field','names','field_22785',1))
  hooks=json.loads((ROOT/'extensions/modules/dynamic-translation/module.json').read_text('utf8'))['hooks']
  act=[x for x in hooks if x['id']=='game-actionbar-display']
  self.assertEqual(len(act),1)
  self.assertEqual((act[0]['class'],act[0]['method'],act[0]['descriptor'],act[0]['kind'],act[0]['argument']),
   ('net/minecraft/class_329','method_1758','(Lnet/minecraft/class_2561;Z)V','argument',0))
 def test_reviewed_item_names_and_quest_names_reuse_glossary(self):
  for k,v in {'Iron Axe':'鐵斧','Tactical Boots':'戰術長靴','Katana du Chasseur':'獵人武士刀',
              'Lingot de Fer Chaud':'灼熱鐵錠','Coal Ore':'煤礦石'}.items():
   self.assertEqual(self.d['items'].get(k),v,k)
  for k,v in {'Dungeon Guard':'地城守衛','Geldorack Dungeon Key':'格爾多拉克地城鑰匙'}.items():
   self.assertEqual(self.d['names'].get(k) or self.d['items'].get(k),v,k)
 def test_recognized_important_english_messages_are_reviewed(self):
  for k,v in {
   'Craft fully complete!':'製作已全部完成！',
   'Step completed! You retrieved the items.':'工序完成！已取回物品。',
   'Wrong tool: axe required.':'工具不符: 需要斧頭。',
   'Wrong tool: pickaxe required.':'工具不符: 需要鎬子。',
   'QTE ready! Strike the anvil to the beat.':'QTE 已就緒！配合節奏敲擊鐵砧。',
   "That's not the right material, or you already added enough!":'材料不符，或已放入足夠材料！',
  }.items():self.assertEqual(self.d['messages'].get(k),v,k)
 def test_source_restrictions_do_not_intercept_user_chat_or_container_data(self):
  from pathlib import Path
  s=(ROOT/'client-framework/src/sao/zh/engine/Script.java').read_text('utf8')
  col=(ROOT/'client-framework/src/sao/zh/engine/NamedContainerTitles.java').read_text('utf8')
  self.assertIn('named-container:visible-title',s)
  self.assertIn('dynamic-translation:game-actionbar-display',s)
  self.assertIn('Forge - Categories',col)
  self.assertNotIn('player.getName()',col)
 def test_npc_name_collection_is_scoped_to_dialogue_render(self):
  doc=json.loads((ROOT/'extensions/modules/npc-dialogue/module.json').read_text('utf8'))
  name=next(h for h in doc['hooks'] if h['id']=='speaker')
  self.assertEqual(name.get('capture'),'npc_name')
  self.assertEqual(name['class'],'fr/zapdialogue/client/gui/BubbleRenderer')
  collector=(ROOT/'client-framework/src/sao/zh/engine/UnknownCollector.java').read_text('utf8')
  self.assertIn('npc_name',collector)
  self.assertIn('USERNAME.matcher(s).matches()',collector)
 def test_version_integrity(self):
  ver=json.loads((ROOT/'languages/zh_tw/zh-tw-version.json').read_text('utf8'))
  content=(ROOT/'languages/zh_tw/translations.json').read_bytes()
  self.assertEqual(ver['version'],'1.0.24')
  self.assertEqual(ver['translation']['size_bytes'],len(content))
  self.assertEqual(ver['translation']['sha256'],hashlib.sha256(content).hexdigest())
if __name__=='__main__':unittest.main()
