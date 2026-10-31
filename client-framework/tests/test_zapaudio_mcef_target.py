from pathlib import Path
import hashlib,json,os,subprocess,unittest,zipfile
ROOT=Path(__file__).resolve().parents[2]
MODULE=ROOT/'extensions/modules/zapaudio-webui/module.json'
class InstalledMcefAgentTarget(unittest.TestCase):
 @unittest.skipUnless(os.environ.get('SAO_ZAPAUDIO_JAR') and os.environ.get('SAO_MCEF_JAR'),'Installed MCEF/ZAP mod not supplied')
 def test_exact_installed_mcef_and_native_extractor_abi(self):
  zpath=Path(os.environ['SAO_ZAPAUDIO_JAR']);mpath=Path(os.environ['SAO_MCEF_JAR'])
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
  h=json.loads(MODULE.read_text('utf8'))['hooks'][0]
  self.assertEqual(h['kind'],'web_extract')
  self.assertEqual(h['descriptor'],'()Ljava/nio/file/Path;')
 def test_runtime_contains_locale_but_never_installs_fabric_addon(self):
  runtime=os.environ.get('SAO_ZAPAUDIO_RUNTIME_JAR','')
  if not runtime or not Path(runtime).is_file():self.skipTest('New external Agent Runtime not supplied')
  with zipfile.ZipFile(runtime) as z:
   names=z.namelist()
   self.assertIn('assets/saomczh_zapaudio/web/saomczh-zh-tw.js',names)
   self.assertIn('sao/zh/engine/WebUiLocaleInstaller.class',names)
   self.assertFalse(any(x.startswith('web/') for x in names))
   self.assertFalse(any(x.endswith(('voice-client.js','music-player.js','ambiance-player.js')) for x in names))
   self.assertNotIn('fabric.mod.json',names)
   self.assertNotIn('saomczh-zapaudio.mixins.json',names)
if __name__=='__main__':unittest.main()
