from pathlib import Path
import json,os,re,unittest,zipfile
ROOT=Path(__file__).resolve().parents[2]
RESOURCE=ROOT/'client-framework/resources/saomczh_zapaudio/web/saomczh-zh-tw.js'
MODULE=ROOT/'extensions/modules/zapaudio-webui/module.json'
class AgentWebUiLocalization(unittest.TestCase):
 def test_all_original_web_i18n_keys_have_chinese(self):
  script=RESOURCE.read_text('utf8')
  match=re.search(r'const EN_ZH = Object\.freeze\((\{.*?\})\);',script,re.S)
  self.assertIsNotNone(match)
  translated=json.loads(match.group(1))
  self.assertEqual(translated['Connected'],'已連線')
  self.assertEqual(translated['Nearby players'],'附近玩家')
  self.assertEqual(translated['Audio Settings'],'音訊設定')
  self.assertIn('Voice range',translated)
  self.assertIn('Mic off',translated)
  self.assertIn('Reconnecting (%1/%2)...',translated)
  self.assertIn('The minimum between your range and the other player',next(k for k in translated if k.startswith('The minimum between')))
  jar=os.environ.get('SAO_ZAPAUDIO_JAR')
  if jar:
   with zipfile.ZipFile(jar) as z:s=z.read('web/index-template.html').decode('utf8')
   raw=re.search(r'var FR_TO_EN = (\{.*?\});',s,re.S).group(1)
   raw=re.sub(r'(?m)^\s*//.*$','',raw)
   originals=json.loads(raw)
   self.assertEqual(set(originals.values())-set(translated),set(),'Missing upstream dynamic message translations')
 def test_one_allowlisted_agent_hook_without_external_mod(self):
  cfg=json.loads(MODULE.read_text('utf8'))
  self.assertEqual(cfg['id'],'zapaudio-webui')
  self.assertEqual(len(cfg['hooks']),1)
  h=cfg['hooks'][0]
  self.assertEqual((h['class'],h['method'],h['kind'],h['descriptor']),
   ('fr/saofr/zapaudio/client/web/WebClientExtractor','extract','web_extract','()Ljava/nio/file/Path;'))
  script=(ROOT/'client-framework/src/sao/zh/engine/Script.java').read_text('utf8')
  self.assertIn('zapaudio-webui:extracted-html',script)
  engine=(ROOT/'client-framework/src/sao/zh/engine/Engine.java').read_text('utf8')
  self.assertIn('WebUiLocaleInstaller.install',engine)
  self.assertFalse((ROOT/'client-framework/addons/zapaudio-webui/fabric.mod.json').exists())
 def test_html_labels_only_and_device_names_unmodified(self):
  script=RESOURCE.read_text('utf8')
  self.assertIn('zapLang',script)
  self.assertIn('window.ZAP_T',script)
  self.assertIn("document.addEventListener('DOMContentLoaded'",script)
  self.assertIn("querySelectorAll('[data-i18n]')",script)
  self.assertIn("querySelectorAll('[data-i18n-tooltip]')",script)
  for bad in ('#audioInputSelect option','#audioOutputSelect option','getUserMedia','RTCPeerConnection','new WebSocket','document.body.innerHTML ='):
   self.assertNotIn(bad,script)
 def test_original_web_cache_is_verified_and_atomic(self):
  installer=(ROOT/'client-framework/src/sao/zh/engine/WebUiLocaleInstaller.java').read_text('utf8')
  self.assertIn('c068d2aeb93478b0d49a1953aeee58f906debb78651d89ea79ad25564338cd02',installer)
  self.assertIn('saomczh-zh-tw.js',installer)
  self.assertIn('ATOMIC_MOVE',installer)
  self.assertIn('Files.isSymbolicLink',installer)
 def test_minecraft_native_language_unchanged(self):
  self.assertFalse((ROOT/'client-framework/addons/zapaudio-webui/assets/zapaudioclient/lang/zh_tw.json').exists())
  self.assertFalse((ROOT/'client-framework/addons/zapaudio-webui/saomczh-zapaudio.mixins.json').exists())
if __name__=='__main__':unittest.main()
