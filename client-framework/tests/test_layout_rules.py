from pathlib import Path
import sys,unittest
sys.path.insert(0,str(Path(__file__).parents[1]/'tools'))
from compose_modules import compose,ModuleError
W={'id':'wrap','class':'fixture/Canvas','method':'wrap','descriptor':'(Ljava/lang/String;FZFI)Ljava/util/List;','kind':'wrap','measure':'width','context_fields':{'font':'Ljava/lang/String;','scale':'F'}}
R={'id':'refresh','class':'fixture/Screen','method':'render','descriptor':'()V','kind':'refresh','cache_fields':[{'name':'labels','descriptor':'Ljava/util/Map;','action':'clear_map'}]}
def build(h):return compose([{'schema_version':1,'id':'ui-text-layout','version':'1.0.0','hooks':h}],version='1.2.0',sequence=102,expires=2000000000)
class LayoutRules(unittest.TestCase):
 def test_shared_module_wrap_and_refresh(self):
  b=build([W,R]);self.assertEqual(b['schema_version'],2);self.assertEqual(len(b['hooks']),2)
 def test_old_rules_stay_schema_one(self):self.assertEqual(build([])['schema_version'],1)
 def test_only_maps_and_keys_can_be_invalidated(self):
  for field in [{'name':'inventory','descriptor':'Ljava/util/List;','action':'clear_map'},{'name':'balance','descriptor':'I','action':'reset_key'}]:
   with self.assertRaises(ModuleError):build([dict(R,cache_fields=[field])])
 def test_empty_refresh_rejected(self):
  with self.assertRaises(ModuleError):build([dict(R,cache_fields=[])])
 def test_bad_width_contract_rejected(self):
  with self.assertRaises(ModuleError):build([dict(W,descriptor='()Ljava/lang/String;')])
 def test_duplicate_refresh_fields_rejected(self):
  with self.assertRaises(ModuleError):build([dict(R,cache_fields=R['cache_fields']*2)])
 def test_wrong_context_type_rejected(self):
  with self.assertRaises(ModuleError):build([dict(W,context_fields={'font':'Ljava/lang/Object;'})])
if __name__=='__main__':unittest.main()
