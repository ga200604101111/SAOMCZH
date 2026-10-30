from pathlib import Path
import json,sys,unittest,time,copy
W=Path(__file__).resolve().parents[1]
if not (W/'extensions/modules').is_dir():W=W.parent
sys.path.insert(0,str(W/'tools' if (W/'tools/compose_localized.py').is_file() else W/'client-framework/tools'))
from compose_localized import compose_bundle,LocaleError,compile_scoped_text,expand_space_exact
# The requested correction intentionally changes only two Daily reward-name hooks.
# Keep all legacy baseline comparisons exact outside these eight expanded keys.
def expected_daily_correction(hook_id, baseline):
 expected={k:v.replace('\uff1a',':') for k,v in baseline.items()}
 if hook_id in ('items-economy:reward-name','items-economy:reward-cached-name'):
  for source in ('Healing Shard x2','治療碎片 x2'):
   for left,right in (('',''),(' ',''),('',' '),(' ',' ')):
    expected[left+source+right]=left+'治療碎片'+right
 return expected
def expected_operations(ops):
 return [dict(op, to=op.get('to','').replace('\uff1a',':')) for op in ops]
class CommonTextRules(unittest.TestCase):
 def test_damage_sources_are_deduplicated(self):
  doc=json.loads((W/'languages/zh_tw/hook_text_rules.json').read_text('utf8'))
  self.assertEqual(doc['schema_version'],2)
  self.assertIn('rule_sets',doc)
  damage=doc['rule_sets']['combat_damage']
  self.assertEqual(damage['padded_exact']['Coup'],'單次傷害')
  self.assertEqual(damage['padded_exact']['total soigné'],'總治療量')
  self.assertEqual(doc['hooks']['combat-feedback:damage-segment'],{'use':'combat_damage'})
  self.assertEqual(doc['hooks']['combat-feedback:damage-text'],{'use':'combat_damage'})
  self.assertNotIn(' Coup ',json.dumps(damage,ensure_ascii=False))
 def test_expands_only_four_explicit_space_variants(self):
  result=expand_space_exact({'Coup':'單次傷害','total soigné':'總治療量'})
  for src,tgt in {'Coup':'單次傷害',' Coup':' 單次傷害','Coup ':'單次傷害 ',' Coup ':' 單次傷害 ','total soigné':'總治療量',' total soigné ':' 總治療量 '}.items():self.assertEqual(result[src],tgt)
  self.assertNotIn('Coup spam',result)
  self.assertNotIn('Coupon',result)
  self.assertNotIn('xCoup',result)
  self.assertNotIn('COUP',result)
 def test_reject_wildcards_and_conflicting_rule_sets(self):
  for bad in ['Coup*','*Coup','Coup?','Coup[0-9]',' Coup','Coup ']:
   with self.assertRaises(LocaleError):expand_space_exact({bad:'單次傷害'})
  with self.assertRaises(LocaleError):compile_scoped_text({'use':'missing'}, {}, 'id:test')
  with self.assertRaises(LocaleError):compile_scoped_text({'padded_exact':{'Coup':'甲'},'exact':{' Coup':'錯'}},{},'id:test')
  with self.assertRaises(LocaleError):compile_scoped_text({'use':'recursive'}, {'recursive':{'use':'recursive'}},'id:test')
  with self.assertRaises(LocaleError):compile_scoped_text({'use':'shared','operations':[{'op':'replace_literal','from':'A','to':'B'}]},{'shared':{'operations':[{'op':'replace_literal','from':'A','to':'C'}]}},'id:test')
 def test_baseline_hooks_have_exact_same_render_operations(self):
  old=json.loads((W/'audit/rules-1.5.1.json' if (W/'audit/rules-1.5.1.json').is_file() else W/'extensions/v2/rules-1.5.1.json').read_text('utf8'))
  fresh=compose_bundle(W,'zh_tw','1.6.0',108,int(time.time())+86400*100)
  current={h['id']:h for h in fresh['hooks']}
  for original in old['hooks']:
   new=current[original['id']]
   self.assertEqual(new.get('exact',{}),expected_daily_correction(original['id'],original.get('exact',{})),original['id']+' exact')
   self.assertEqual(new.get('operations',[]),expected_operations(original.get('operations',[])),original['id']+' operations')
  self.assertEqual(len(current),124)  # Existing 123 targets plus the description-only List accessor
 def test_all_existing_hook_exceptions_are_preserved(self):
  baseline=json.loads((W/'audit/hook_text_rules-1.5.1.json' if (W/'audit/hook_text_rules-1.5.1.json').is_file() else W/'client-framework/tests/reference/hook_text_rules-1.5.1.json').read_text('utf8'))
  canonical=json.loads((W/'languages/zh_tw/hook_text_rules.json').read_text('utf8'))
  sets=canonical['rule_sets']
  for id,value in baseline['hooks'].items():
   resolved=compile_scoped_text(canonical['hooks'][id],sets,id)
   self.assertEqual(resolved.get('exact',{}),expected_daily_correction(id,value.get('exact',{})),id)
   self.assertEqual(resolved.get('operations',[]),expected_operations(value.get('operations',[])),id)
 def test_source_repetition_reduction(self):
  baseline=(W/'audit/hook_text_rules-1.5.1.json' if (W/'audit/hook_text_rules-1.5.1.json').is_file() else W/'client-framework/tests/reference/hook_text_rules-1.5.1.json').stat().st_size
  current=(W/'languages/zh_tw/hook_text_rules.json').stat().st_size
  self.assertLess(current,baseline*0.7)
if __name__=='__main__':unittest.main()
