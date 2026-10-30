"""Level abbreviations are UI tokens, not Chinese sentence punctuation."""
from pathlib import Path
import copy,json,os,re,sys,unittest,zipfile
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'client-framework/tools'))
import build_glyph_contract_pack as builder

EXPECTED={'saoui.node.level':'Lv. %s+','saoui.mob.level':'LV. %s','companionsmod.ui.level':'Lv. %s'}
class LevelAbbreviation(unittest.TestCase):
 def test_restore_original_level_tokens(self):
  for key,target in EXPECTED.items():
   for bad in (target.replace('.','。'),target.replace('.','．')):
    with self.subTest(key=key,bad=bad):
     source={key:bad};self.assertEqual(builder.encode_values(source,{}),{key:target})
 def test_companion_case_matches_original_en_us(self):
  self.assertEqual(builder.encode_values({'companionsmod.ui.level':'LV。 %s'},{}),{'companionsmod.ui.level':'Lv. %s'})
 def test_preserve_source_and_placeholder_and_plus(self):
  source={'saoui.node.level':'Lv。 %s+','saoui.mob.level':'LV。 %s'};saved=copy.deepcopy(source)
  result=builder.encode_values(source,{})
  self.assertEqual(source,saved)
  for key,value in result.items():
   self.assertEqual(re.findall(r'%[a-z]',value),['%s'])
   self.assertEqual(value.count('+'),source[key].count('+'))
  self.assertEqual(result['saoui.node.level'] % '1','Lv. 1+')
 def test_no_global_sentence_or_numeric_rewrite(self):
  source={'story':'等級提升了。','other':'Lv。 %s+','ratio':'1.5%','saoui.node.level':'等級 %s+','saoui.mob.level':'LV。 %d'}
  self.assertEqual(builder.encode_values(source,{}),source)
 def test_correct_labels_are_idempotent(self):
  self.assertEqual(builder.encode_values(EXPECTED,{}),EXPECTED)
 def test_all_already_valid_level_spellings_are_untouched(self):
  for value in ("Lv. %s+","LV. %s+","Lv.%s+","Lv. %1$s+","Lv. 1+","LV. 99","Lv. 100+"):
   for key in EXPECTED:
    with self.subTest(key=key,value=value):self.assertEqual(builder.encode_values({key:value},{}),{key:value})
 @unittest.skipUnless(os.environ.get('SAO_GLYPH_PACK'),'External resource pack not supplied')
 def test_actual_localized_pack(self):
  seen=0
  with zipfile.ZipFile(os.environ['SAO_GLYPH_PACK']) as z:
   for name in z.namelist():
    if '/lang/' not in name or not name.endswith(('/zh_tw.json','/zh_hk.json')):continue
    doc=json.loads(z.read(name))
    for key,target in EXPECTED.items():
     if key in doc:
      seen+=1
      # Existing non-Lv companion label is outside this rollback; preserve it.
      expected="等級 %s" if name=="assets/companionsmod/lang/zh_tw.json" and key=="companionsmod.ui.level" else target
      with self.subTest(file=name,key=key):self.assertEqual(doc[key],expected)
  self.assertGreaterEqual(seen,5)

if __name__=='__main__':unittest.main()
