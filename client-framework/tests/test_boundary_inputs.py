import sys,unittest,copy
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from compose_modules import compose,ModuleError

def module(**changes):return dict({'schema_version':1,'id':'items','version':'1.0.0','hooks':[]},**changes)
def run(rows):return compose(rows,version='1.1.0',sequence=101,expires=2000000000)

class BoundaryInputs(unittest.TestCase):
 def test_tests_are_bounded_array(self):
  for value in (None,42,'bad',{},[{}]*513):
   with self.subTest(value=type(value).__name__):
    with self.assertRaises(ModuleError):run([module(tests=value)])
 def test_sample_values_must_be_strings(self):
  h={'id':'name','class':'sample/Screen','method':'name','descriptor':'()Ljava/lang/String;','kind':'return'}
  for field in ('hook','input','expect'):
   case={'hook':'name','input':'A','expect':'甲'};case[field]=42
   with self.subTest(field=field):
    with self.assertRaises(ModuleError):run([module(hooks=[h],tests=[case])])
 def test_schema_boolean_not_integer(self):
  with self.assertRaises(ModuleError):run([module(schema_version=True)])
 def test_dependency_keys_checked(self):
  with self.assertRaises(ModuleError):run([module(dependencies={7:'1.0.0'})])
 def test_very_large_dictionary_output_rejected(self):
  h={'id':'name','class':'sample/Screen','method':'name','descriptor':'()Ljava/lang/String;','kind':'return'}
  with self.assertRaises(ModuleError):run([module(hooks=[h],dictionary={'names':{'A':'B'*16385}})])
 def test_expired_release_not_composed(self):
  with self.assertRaises(ModuleError):compose([module()],version='1.1.0',sequence=101,expires=1)
 def test_invalid_operations_container_rejected(self):
  h={'id':'name','class':'sample/Screen','method':'name','descriptor':'()Ljava/lang/String;','kind':'return','operations':{'op':'prefix'}}
  with self.assertRaises(ModuleError):run([module(hooks=[h])])

if __name__=='__main__':unittest.main()
