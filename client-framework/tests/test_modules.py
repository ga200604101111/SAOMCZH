import copy,importlib.util,json,sys,time,unittest
from pathlib import Path
W=Path(__file__).resolve().parents[1];sys.path.insert(0,str(W/'tools'))
if importlib.util.find_spec('compose_modules'):
 from compose_modules import compose,ModuleError,load_json
else:
 class ModuleError(ValueError):pass
 def compose(*a,**k):raise AssertionError('Missing domain-module composer: extensions are still an ungrouped list of hooks')
 def load_json(*a):raise AssertionError('Missing strict JSON loader')

EXP=2000000000

def hook(name='render',id='title'):
 return {'id':id,'class':'fixture/DomainScreen','method':name,'descriptor':'()Ljava/lang/String;','kind':'return','locale':'*','section':'names'}
def mod(id='items',hooks=None,**kw):
 return dict({'schema_version':1,'id':id,'version':'1.0.0','name':id,'dependencies':{},'hooks':hooks if hooks is not None else [hook()]},**kw)
def compile(rows,seq=101):return compose(rows,version='1.1.0',sequence=seq,expires=EXP)

class Domains(unittest.TestCase):
 def test_multi_interface_one_module(self):
  result=compile([mod(hooks=[hook('tooltip','tooltip'),hook('reward','reward')])])
  self.assertEqual(len(result['hooks']),2);self.assertEqual(len(result['module_catalog']),1)
  self.assertEqual(result['hooks'][0]['id'],'items:reward')
 def test_dependency_dictionary_and_local_override(self):
  shared=mod('common',[],dictionary={'names':{'Stamina Shard':'耐力碎片','Class':'職業'}})
  item=mod(dependencies={'common':'1.0.0'},dictionary={'names':{'Class':'職業需求'}})
  r=compile([item,shared]);h=r['hooks'][0]
  self.assertEqual(h['exact']['Stamina Shard'],'耐力碎片');self.assertEqual(h['exact']['Class'],'職業需求')
 def test_order_determinism_and_no_input_mutation(self):
  rows=[mod('quests',[hook('quest')]),mod('items',[hook('item')])];before=copy.deepcopy(rows)
  self.assertEqual(compile(rows),compile(rows[::-1]));self.assertEqual(rows,before)
 def test_scope_does_not_leak_between_modules(self):
  r=compile([mod('a',[hook('a')],dictionary={'names':{'X':'甲'}}),mod('b',[hook('b')])])
  self.assertIn('X',r['hooks'][0]['exact']);self.assertNotIn('X',r['hooks'][1]['exact'])
 def test_disabled_module_omitted(self):
  r=compile([mod('a',[hook('a')]),mod('b',[hook('b')],enabled=False)])
  self.assertEqual(len(r['hooks']),1)
 def test_missing_dependency_rejected(self):
  with self.assertRaises(ModuleError):compile([mod(dependencies={'common':'1.0.0'})])
 def test_version_floor_rejected(self):
  with self.assertRaises(ModuleError):compile([mod('common',[]),mod(dependencies={'common':'2.0.0'})])
 def test_cycle_rejected(self):
  with self.assertRaises(ModuleError):compile([mod('a',[],dependencies={'b':'1.0.0'}),mod('b',[],dependencies={'a':'1.0.0'})])
 def test_double_hook_same_site_rejected(self):
  with self.assertRaises(ModuleError):compile([mod('a'),mod('b')])
 def test_dependency_conflict_is_explicit(self):
  a=mod('a',[],dictionary={'names':{'X':'甲'}});b=mod('b',[],dictionary={'names':{'X':'乙'}})
  with self.assertRaises(ModuleError):compile([a,b,mod('c',dependencies={'a':'1.0.0','b':'1.0.0'})])
 def test_placeholder_loss_rejected(self):
  with self.assertRaises(ModuleError):compile([mod(dictionary={'names':{'Level %s':'等級'}})])
 def test_protected_target_rejected(self):
  h=hook();h['class']='java/lang/String'
  with self.assertRaises(ModuleError):compile([mod(hooks=[h])])
 def test_unbounded_operation_rejected(self):
  h=hook();h['operations']=[{'op':'javascript','from':'while(true)','to':''}]
  with self.assertRaises(ModuleError):compile([mod(hooks=[h])])
 def test_too_many_hooks_rejected(self):
  with self.assertRaises(ModuleError):compile([mod(hooks=[hook('f'+str(i),'h'+str(i)) for i in range(129)])])
 def test_individual_module_rollback_preserves_other_updates(self):
  a=mod('items',[hook('items')],dictionary={'names':{'X':'舊物品'}})
  b=mod('quests',[hook('quests')],version='2.0.0',dictionary={'names':{'Y':'新任務'}})
  r=compile([a,b],103)
  self.assertEqual(r['hooks'][0]['exact']['X'],'舊物品');self.assertEqual(r['hooks'][1]['exact']['Y'],'新任務')
  self.assertEqual(r['module_catalog'][1]['version'],'2.0.0')
 def test_duplicate_json_keys_rejected(self):
  with self.assertRaises(ModuleError):load_json('{"id":"a","id":"b"}')
 def test_higher_source_priority_is_explicit(self):
  h=hook();h['exact']={'X':'特例'}
  self.assertEqual(compile([mod(hooks=[h],dictionary={'names':{'X':'常規'}})])['hooks'][0]['exact']['X'],'特例')
 def test_source_chinese_text_not_a_translation_failure(self):
  r=compile([mod(dictionary={'names':{'已翻譯':'已翻譯'}})])
  self.assertEqual(r['hooks'][0]['exact']['已翻譯'],'已翻譯')

if __name__=='__main__':unittest.main()
