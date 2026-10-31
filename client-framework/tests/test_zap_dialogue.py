from pathlib import Path
import json,unittest
R=Path(__file__).resolve().parents[2]
class NPCDialogueLocalization(unittest.TestCase):
 def test_verified_zap_dialogue_client_hooks(self):
  doc=json.loads((R/'extensions/modules/npc-dialogue/module.json').read_text('utf8'))
  self.assertEqual(doc['id'],'npc-dialogue')
  hooks={h['id']:h for h in doc['hooks']}
  self.assertEqual(set(hooks),{'speaker','text-wrap','text-trim','text-length'})
  self.assertEqual(hooks['speaker']['class'],'fr/zapdialogue/client/gui/BubbleRenderer')
  self.assertEqual(hooks['speaker']['method'],'drawSpeakerName')
  self.assertEqual(hooks['speaker']['argument'],2)
  self.assertEqual(hooks['speaker']['section'],'names')
  for key,method in [('text-wrap','wrap'),('text-trim','trim'),('text-length','length')]:
   h=hooks[key]
   self.assertEqual(h['class'],'fr/zapdialogue/client/text/StyledText')
   self.assertEqual(h['method'],method)
   self.assertEqual(h['argument'],0)
   self.assertEqual(h['kind'],'argument')
   self.assertEqual(h['section'],'lines')
  self.assertEqual(hooks['text-wrap'].get('capture'),'dialogue')
  self.assertFalse(any('network/' in h['class'] or 'packet' in h['class'].lower() for h in hooks.values()))
 def test_original_screenshot_has_reviewed_exact_translations(self):
  d=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  self.assertEqual(d['names'].get('Shepherd Ivo'),'牧羊人伊沃')
  self.assertEqual(d['lines'].get('The pens are holding and my animals sleep. That is thanks to you.'),
     '圍欄守住了，我的牲畜也睡得安穩。這都多虧了你。')
  self.assertNotIn('YingHuang09',d['names'])
 def test_locale_version_updated_with_valid_digest(self):
  import hashlib
  p=R/'languages/zh_tw/translations.json';manifest=json.loads((R/'languages/zh_tw/zh-tw-version.json').read_text('utf8'))
  self.assertEqual(manifest['version'],'1.0.24')
  self.assertEqual(manifest['translation']['sha256'],hashlib.sha256(p.read_bytes()).hexdigest())
  self.assertEqual(manifest['translation']['size_bytes'],p.stat().st_size)
 def test_agent_only_scope_and_capture(self):
  script=(R/'client-framework/src/sao/zh/engine/Script.java').read_text('utf8')
  collector=(R/'client-framework/src/sao/zh/engine/UnknownCollector.java').read_text('utf8')
  renderer=(R/'client-framework/src/sao/zh/engine/RenderTextPipeline.java').read_text('utf8')
  self.assertIn('fr/zapdialogue/client/text/StyledText',script)
  self.assertIn('fr/zapdialogue/client/gui/BubbleRenderer',script)
  self.assertIn('if(origin.equals("dialogue"))',collector)
  self.assertIn('owner.startsWith("fr/zapdialogue/client/")',renderer)
  self.assertFalse((R/'client-framework/addons/zapdialogue/fabric.mod.json').exists())
 def test_bytecode_rule_budget(self):
  from pathlib import Path
  import time,sys
  sys.path.insert(0,str(R/'client-framework/tools'))
  from compose_localized import compose_bundle
  d=compose_bundle(R,'zh_tw','1.13.0',117,int(time.time())+86400)
  self.assertEqual(len(d['hooks']),137)
  targets={h['id']:h for h in d['hooks'] if h['id'].startswith('npc-dialogue:')}
  self.assertEqual(len(targets),4)
  self.assertEqual(targets['npc-dialogue:text-wrap'].get('capture'),'dialogue')
if __name__=='__main__':unittest.main()
