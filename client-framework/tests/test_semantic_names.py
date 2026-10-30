from pathlib import Path
import json,sys,time,unittest
R=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(R/'client-framework/tools'))
from compose_localized import compose_bundle
from compose_modules import checked_hook,ModuleError
class SemanticNames(unittest.TestCase):
 def test_shared_canonical_names(self):
  d=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  expected={'Mini Treant':'迷你樹人','Treant':'樹人','Local Lumberjack':'當地伐木工','Oak Stump':'橡木樹樁','OAK STUMP':'橡木樹樁','Marécage Putride':'腐敗沼澤'}
  for k,v in expected.items():
   with self.subTest(source=k):self.assertEqual(d['names'].get(k),v)
  self.assertEqual(d['items']['Training Pavise'],'訓練用大盾')
  self.assertEqual(d['names']['Calme Mortel'],'致命沉靜')
 def test_system_messages_are_complete_and_not_player_names(self):
  d=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  for src,target in [('Calme Mortel activé !','致命沉靜已啟用！'),('Calme Mortel déclenché !','致命沉靜已觸發！'),('Calme Mortel épuisé !','致命沉靜已耗盡！'),('Calme Mortel prêt !','致命沉靜已就緒！'),('You are at the spawn.','你已在重生點。')]:
   with self.subTest(source=src):self.assertEqual(d['messages'].get(src),target)
 def test_capture_and_target_boundaries(self):
  b=compose_bundle(R,'zh_tw','1.8.0',110,int(time.time())+86400);h={x['id']:x for x in b['hooks']}
  self.assertEqual(len(h),129)
  self.assertEqual(h['codex-creatures:mob-shown-name']['kind'],'mob_name')
  self.assertEqual(h['items-economy:tooltip-name']['class'],'fr/sao/tooltip/TipModel')
  self.assertEqual(h['dynamic-translation:game-system-display']['kind'],'system_text')
  self.assertNotIn('capture',h['dynamic-translation:game-system-display'])
  self.assertNotIn('capture',h['worlds-locations:npc-tagged-label'])
  for module,id in [('dynamic-translation','game-system-display'),('codex-creatures','mob-shown-name')]:
   raw=next(x for x in json.loads((R/f'extensions/modules/{module}/module.json').read_text('utf8'))['hooks'] if x['id']==id)
   for replacement in [{'class':'net/minecraft/class_338'},{'method':'method_45748'},{'section':'items'},{'capture':'system'}]:
    with self.subTest(module=module,replacement=replacement):
     with self.assertRaises(ModuleError):checked_hook(dict(raw,**replacement),module,{})
 def test_no_per_number_dictionary_expansion(self):
  d=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  for raw in ['Mini Treant Lv. 3','Mini Treant Lv. 4','OAK STUMP Lv ° 1+','Se rendre là où résident les brigands | 2773m','[NPC] Local Lumberjack']:
   self.assertTrue(all(raw not in d[sec] for sec in ('names','items','lines','messages')))
if __name__=='__main__':unittest.main()
