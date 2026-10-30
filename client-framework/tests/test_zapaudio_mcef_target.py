from pathlib import Path
import hashlib,json,os,subprocess,unittest,zipfile
ROOT=Path(__file__).resolve().parents[2]
ADDON=ROOT/'client-framework/addons/zapaudio-webui'
class InstalledMcefTarget(unittest.TestCase):
 @unittest.skipUnless(os.environ.get('SAO_ZAPAUDIO_JAR') and os.environ.get('SAO_MCEF_JAR'),'Installed MCEF/ZAP mod not supplied')
 def test_exact_installed_mcef_and_original_extraction_abi(self):
  zpath=Path(os.environ['SAO_ZAPAUDIO_JAR'])
  mpath=Path(os.environ['SAO_MCEF_JAR'])
  with zipfile.ZipFile(zpath) as z,zipfile.ZipFile(mpath) as m:
   self.assertEqual(json.loads(z.read('fabric.mod.json'))['id'],'zapaudioclient')
   self.assertEqual(json.loads(m.read('fabric.mod.json'))['id'],'mcef')
   self.assertIn('com/cinemamod/mcef/MCEFBrowser.class',m.namelist())
   self.assertIn('org/cef/browser/CefBrowser.class',m.namelist())
   self.assertIn('fr/saofr/zapaudio/client/web/WebClientExtractor.class',z.namelist())
   self.assertIn('fr/saofr/zapaudio/client/screen/BrowserOverlayScreen.class',z.namelist())
   self.assertIn('web/index-template.html',z.namelist())
   self.assertIn('web/voice-client.js',z.namelist())
   self.assertEqual(hashlib.sha256(z.read('web/index-template.html')).hexdigest(),'c068d2aeb93478b0d49a1953aeee58f906debb78651d89ea79ad25564338cd02')
  javap=Path(r'D:/Program Files/Zulu/zulu-26/bin/javap.exe')
  p=subprocess.run([str(javap),'-p','-s','-cp',str(zpath),'fr.saofr.zapaudio.client.web.WebClientExtractor'],capture_output=True,timeout=20)
  self.assertEqual(p.returncode,0)
  out=p.stdout.decode('utf8','replace')
  self.assertIn('public static synchronized java.nio.file.Path extract()',out)
  self.assertIn('descriptor: ()Ljava/nio/file/Path;',out)
  # The exact hook is an extraction RETURN; it never overrides MCEF's browser.
  mixin=(ADDON/'src/sao/zh/zapaudio/mixin/WebClientExtractorMixin.java').read_text('utf8')
  self.assertIn('extract()Ljava/nio/file/Path;',mixin)
  self.assertIn('@At("RETURN")',mixin)
  self.assertNotIn('MCEFBrowser',mixin)
 def test_voice_core_js_are_not_packaged_or_overridden(self):
  artifact=Path(os.environ.get('SAO_ZAPAUDIO_ADDON_JAR',''))
  if not artifact.is_file():self.skipTest('Addon not built')
  with zipfile.ZipFile(artifact) as z:
   names=z.namelist()
   self.assertFalse(any(x.startswith('web/') for x in names))
   self.assertFalse(any(x.endswith(('voice-client.js','music-player.js','ambiance-player.js')) for x in names))
   self.assertEqual(json.loads(z.read('fabric.mod.json'))['id'],'saomczh_zapaudio')
if __name__=='__main__':unittest.main()
