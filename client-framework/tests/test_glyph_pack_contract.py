"""Corpus-wide font asset contract; enable with SAO_GLYPH_PACK=<candidate ZIP>."""
from pathlib import Path
import os,json,zipfile,unittest,io,hashlib
ROOT=Path(__file__).resolve().parents[2]

def han(cp):
 return 0x3400<=cp<=0x4dbf or 0x4e00<=cp<=0x9fff or 0xf900<=cp<=0xfaff or 0x20000<=cp<=0x323af

def strings(node):
 if isinstance(node,str):yield node
 elif isinstance(node,dict):
  for v in node.values():yield from strings(v)
 elif isinstance(node,list):
  for v in node:yield from strings(v)

@unittest.skipUnless(os.environ.get('SAO_GLYPH_PACK'),'External font asset ZIP not supplied')
class GlyphPackContract(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.pack=zipfile.ZipFile(os.environ['SAO_GLYPH_PACK'])
  cls.base=zipfile.ZipFile(os.environ.get('SAO_GLYPH_BASELINE',os.environ['SAO_GLYPH_PACK']))
  cls.atlases={n:json.loads(cls.pack.read(n)) for n in cls.pack.namelist() if n.endswith('.json') and '/msdf/' in n}
  table=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  values=[v for sec in ('names','items','lines','messages') for v in table[sec].values()]
  for n in cls.base.namelist():
   if n.endswith(('/zh_tw.json','/zh_hk.json')) and '/lang/' in n:
    values.extend(strings(json.loads(cls.base.read(n))))
  cls.required={ord(c) for s in values for c in s if han(ord(c))}
  key='assets/saotw/glyph-routing.json'
  cls.routing=json.loads(cls.pack.read(key)) if key in cls.pack.namelist() else {}
  cls.aliases={int(k):v for k,v in cls.routing.get('aliases',{}).items()}
 @classmethod
 def tearDownClass(cls):cls.pack.close();cls.base.close()
 def test_manifest_and_collision_isolation_exist(self):
  self.assertEqual(self.routing.get('schema_version'),1,'No verified text/icon routing contract')
  self.assertIn(ord('田'),self.aliases,'The banner glyph must not be used as the translated field character')
  self.assertGreaterEqual(len(self.aliases),30,'Must cover the full supported collision inventory, not named examples only')
 def test_every_current_translation_character_has_an_atlas_glyph(self):
  self.assertEqual(len(self.atlases),26)
  wanted={self.aliases.get(cp,cp) for cp in self.required}|set(self.aliases.values())|{0xe900,0xe901,0xe902}
  for name,data in self.atlases.items():
   with self.subTest(atlas=name):
    got={g['unicode'] for g in data.get('glyphs',[])}
    missing=wanted-got
    self.assertFalse(missing,'Missing %d glyphs: %s'%(len(missing),' '.join(f'U+{v:X}' for v in sorted(missing)[:20])))
 def test_alias_fonts_do_not_capture_raw_icons(self):
  from fontTools.ttLib import TTFont
  for weight in ('regular','bold'):
   name=f'assets/saotw/font/literal-alias-{weight}.ttf'
   self.assertIn(name,self.pack.namelist())
   if name not in self.pack.namelist():continue
   with TTFont(io.BytesIO(self.pack.read(name))) as font:
    keys=set(font.getBestCmap());self.assertEqual(keys,set(self.aliases.values()))
 def test_original_bitmap_icons_are_preserved(self):
  for name in self.base.namelist():
   if not name.endswith('.json') or '/font/' not in name:continue
   old=json.loads(self.base.read(name));new=json.loads(self.pack.read(name))
   for p in old.get('providers',[]):
    if p.get('type') in ('bitmap','minecraft:bitmap'):
     self.assertIn(p,new.get('providers',[]),name+' changed original icon binding')
 def test_every_font_definition_can_render_the_aliases(self):
  for name in self.base.namelist():
   if not name.endswith('.json') or '/font/' not in name or '/msdf/' in name:continue
   doc=json.loads(self.pack.read(name))
   if 'providers' not in doc:continue
   with self.subTest(font=name):
    self.assertTrue(any(str(p.get('id','')).startswith('saotw:literal_alias/') for p in doc['providers']),name)
 def test_rare_conflicts_remain_explicit_and_outside_translated_corpus(self):
  unsupported=set(self.routing.get('unsupported_icon_codepoints',[]))
  self.assertFalse(unsupported&self.required,'A translated character has no safe font source')
  self.assertFalse(unsupported&set(self.aliases))
 def test_original_textures_and_other_resources_unchanged(self):
  for info in self.base.infolist():
   n=info.filename
   if n=='pack.mcmeta' or n.endswith(('/zh_tw.json','/zh_hk.json')) or '/msdf/' in n or '/font/' in n and n.endswith('.json'):continue
   with self.subTest(resource=n):
    self.assertEqual(self.pack.getinfo(n).CRC,info.CRC)
 def test_manifest_hashes_and_dimensions_are_valid(self):
  for name,sha in self.routing.get('assets_sha256',{}).items():
   self.assertEqual(hashlib.sha256(self.pack.read(name)).hexdigest(),sha,name)
  for name,data in self.atlases.items():
   w=data['atlas']['width'];h=data['atlas']['height'];self.assertLessEqual(max(w,h),8192,name)
   for glyph in data['glyphs']:
    if 'atlasBounds' not in glyph:continue
    b=glyph['atlasBounds'];self.assertTrue(0<=b['left']<=b['right']<=w and 0<=b['top']<=b['bottom']<=h,name)

if __name__=='__main__':unittest.main()
