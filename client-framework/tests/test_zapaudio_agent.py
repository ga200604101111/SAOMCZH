from pathlib import Path
import json,unittest
ROOT=Path(__file__).resolve().parents[2]
MODULE=ROOT/'extensions/modules/zapaudio-webui/module.json'
class AgentWebUi(unittest.TestCase):
 def test_module_is_exactly_one_whitelisted_extractor_path_hook(self):
  doc=json.loads(MODULE.read_text('utf8'))
  self.assertEqual(doc['id'],'zapaudio-webui')
  self.assertEqual(len(doc['hooks']),1)
  h=doc['hooks'][0]
  self.assertEqual((h['id'],h['class'],h['method'],h['descriptor'],h['kind'],h['section']),
   ('extracted-html','fr/saofr/zapaudio/client/web/WebClientExtractor','extract','()Ljava/nio/file/Path;','web_extract','lines'))
  self.assertNotIn('capture',h)
 def test_only_agent_runtime_manages_extraction(self):
  source=ROOT/'client-framework/src/sao/zh/engine'
  script=(source/'Script.java').read_text('utf8')
  engine=(source/'Engine.java').read_text('utf8')
  transformer=(source/'RuleTransformer.java').read_text('utf8')
  installer=(source/'WebUiLocaleInstaller.java').read_text('utf8')
  self.assertIn('case "web_extract"',script)
  self.assertIn('web_extract',transformer)
  self.assertIn('WebUiLocaleInstaller.install',engine)
  self.assertIn('Files.isSymbolicLink',installer)
  self.assertIn('c068d2aeb93478b0d49a1953aeee58f906debb78651d89ea79ad25564338cd02',installer)
 def test_script_is_embedded_in_external_runtime_jar_build(self):
  p=ROOT/'client-framework/resources/saomczh_zapaudio/web/saomczh-zh-tw.js'
  text=p.read_text('utf8')
  self.assertIn('window.ZAP_T',text)
  self.assertIn("querySelectorAll('[data-i18n]')",text)
  self.assertNotIn('RTCPeerConnection',text)
  build=(ROOT/'client-framework/tools/compile_sources.py').read_text('utf8')
  self.assertIn('saomczh-zh-tw.js',build)
 def test_no_standalone_fabric_module_or_addon_installer(self):
  addon=ROOT/'client-framework/addons/zapaudio-webui'
  self.assertFalse((addon/'fabric.mod.json').exists())
  self.assertFalse((addon/'saomczh-zapaudio.mixins.json').exists())
  self.assertFalse((addon/'src/sao/zh/zapaudio/mixin/WebClientExtractorMixin.java').exists())
 def test_existing_sound_of_aincrad_jar_is_not_modified_or_packed(self):
  self.assertFalse((ROOT/'client-framework/resources'/'web/index-template.html').exists())
  self.assertFalse(any((ROOT/'client-framework/resources').rglob('voice-client.js')))
  self.assertFalse(any((ROOT/'client-framework/resources').rglob('music-player.js')))
if __name__=='__main__':unittest.main()
