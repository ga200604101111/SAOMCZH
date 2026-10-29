"""CI guard: new translated characters require a regenerated, verified glyph matrix."""
from pathlib import Path
import json,unittest
ROOT=Path(__file__).resolve().parents[2]
def han(cp):return 0x3400<=cp<=0x4dbf or 0x4e00<=cp<=0x9fff or 0xf900<=cp<=0xfaff or 0x20000<=cp<=0x323af
class DeclaredGlyphCoverage(unittest.TestCase):
 def test_current_dictionary_and_rules_fit_verified_glyph_corpus(self):
  path=ROOT/'client-framework/tests/reference/glyph-coverage.json'
  self.assertTrue(path.is_file(),'Corpus-wide glyph verification manifest is missing')
  reference=json.loads(path.read_text('utf8'));supported=set(reference['corpus_codepoints'])
  table=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  values=[v for section in ('names','items','lines','messages') for v in table[section].values()]
  rules=json.loads((ROOT/'languages/zh_tw/hook_text_rules.json').read_text('utf8'))
  for spec in list(rules.get('rule_sets',{}).values())+list(rules.get('hooks',{}).values()):
   for key in ('exact','padded_exact'):values.extend(spec.get(key,{}).values())
   values.extend(op.get('to','') for op in spec.get('operations',[]))
  used={ord(c) for value in values for c in value if han(ord(c))}
  missing=used-supported
  self.assertFalse(missing,'Rebuild font matrix for new characters: '+''.join(chr(c) for c in sorted(missing)))
  self.assertEqual(reference['atlas_count'],26)
  self.assertFalse(set(reference['unsupported_icon_codepoints'])&used)
if __name__=='__main__':unittest.main()
