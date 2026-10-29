from pathlib import Path
import unittest,zipfile,json,os,io
from fontTools.ttLib import TTFont
R=Path('D:/翻譯');P=Path(os.environ.get('SAO_PACK_TEST',str(R/'SAO_ZH_Font_Public_Build/SAO_ZH_Pack_v1.0.5_font_reload_repair.zip')))
class FontRepair(unittest.TestCase):
 def test_affected_characters_use_complete_fallback(self):
  with zipfile.ZipFile(P) as z:
   hits=[]
   for name in z.namelist():
    if not name.endswith('.json') or '/msdf/' not in name:continue
    data=json.loads(z.read(name));present={x.get('unicode') for x in data.get('glyphs',[])}
    if present&{ord(x) for x in '訓氾嶄'}:hits.append(name)
   self.assertFalse(hits,'Small MSDF glyphs still override the complete native CJK fallback: '+repr(hits))
 def test_complete_font_contains_requested_characters(self):
  with zipfile.ZipFile(P) as z:
   for weight in ['regular','bold']:
    f=TTFont(io.BytesIO(z.read('assets/minecraft/font/sao_cjk_'+weight+'.ttf')));c=f.getBestCmap();self.assertTrue(all(ord(x) in c for x in '訓氾嶄'));f.close()
 def test_requested_projectile_label(self):
  with zipfile.ZipFile(P) as z:self.assertEqual(json.loads(z.read('assets/saoui/lang/zh_tw.json'))['saoui.titles.stat.projectile-damage'],'投射傷害')
 def test_damage_localization_asset(self):
  with zipfile.ZipFile(P) as z:
   d=json.loads(z.read('assets/saoui/actionbar_translations.json'))['en'];self.assertEqual(d['Coup'],'單次傷害');self.assertEqual(d.get('total'),'總傷害');self.assertEqual(d['total soigné'],'總治療量')
 def test_zip_integrity(self):
  with zipfile.ZipFile(P) as z:self.assertIsNone(z.testzip())
if __name__=='__main__':unittest.main()
