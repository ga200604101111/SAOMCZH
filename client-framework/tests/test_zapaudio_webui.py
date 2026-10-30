from pathlib import Path
import json,os,re,unittest,zipfile
ROOT=Path(__file__).resolve().parents[2]
PACKAGE=ROOT/'client-framework/addons/zapaudio-webui'
class WebUiLocalization(unittest.TestCase):
 def test_static_and_live_keys_cover_original_web_i18n(self):
  script=(PACKAGE/'assets/saomczh_zapaudio/web/saomczh-zh-tw.js').read_text('utf8')
  m=re.search(r'const EN_ZH = Object\.freeze\((\{.*?\})\);',script,re.S)
  self.assertIsNotNone(m)
  translated=json.loads(m.group(1))
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
   self.assertEqual(set(originals.values())-set(translated),set(),'Untranslated official dynamic/status keys')
 def test_mixin_only_targets_extractor_and_never_updates_original_mod(self):
  cfg=json.loads((PACKAGE/'fabric.mod.json').read_text('utf8'))
  self.assertEqual(cfg['id'],'saomczh_zapaudio')
  self.assertIn('zapaudioclient',cfg['depends'])
  x=json.loads((PACKAGE/'saomczh-zapaudio.mixins.json').read_text('utf8'))
  self.assertEqual(x['client'],['WebClientExtractorMixin'])
  java=(PACKAGE/'src/sao/zh/zapaudio/mixin/WebClientExtractorMixin.java').read_text('utf8')
  self.assertIn('WebClientExtractor',java)
  self.assertIn('RETURN',java)
  self.assertNotIn('WebSocket',java)
 def test_locale_only_rewrites_visible_ui_without_touching_device_names(self):
  script=(PACKAGE/'assets/saomczh_zapaudio/web/saomczh-zh-tw.js').read_text('utf8')
  self.assertIn('zapLang',script)
  self.assertIn('window.ZAP_T',script)
  self.assertIn("document.addEventListener('DOMContentLoaded'",script)
  self.assertIn("querySelectorAll('[data-i18n]')",script)
  self.assertIn("querySelectorAll('[data-i18n-tooltip]')",script)
  for bad in ('#audioInputSelect option','#audioOutputSelect option','getUserMedia','RTCPeerConnection','new WebSocket','document.body.innerHTML ='):
   self.assertNotIn(bad,script)
 def test_original_template_sha_guard_and_reversible_install(self):
  java=(PACKAGE/'src/sao/zh/zapaudio/WebUiLocaleInstaller.java').read_text('utf8')
  self.assertIn('c068d2aeb93478b0d49a1953aeee58f906debb78651d89ea79ad25564338cd02',java)
  self.assertIn('saomczh-zh-tw.js',java)
  self.assertIn('ATOMIC_MOVE',java)
  self.assertIn('Files.isSymbolicLink',java)
 def test_minecraft_settings_locale_keys_and_device_option_exclusion(self):
  d=json.loads((PACKAGE/'assets/zapaudioclient/lang/zh_tw.json').read_text('utf8'))
  self.assertIn('key.zapaudioclient.push_to_talk',d)
  self.assertIn('%s',d['zapaudioclient.chat_audio_started'])
  self.assertTrue(all('：' not in v for v in d.values()))
if __name__=='__main__':unittest.main()
