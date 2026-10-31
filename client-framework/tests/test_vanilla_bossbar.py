from pathlib import Path
import json,hashlib,unittest,time,sys
R=Path(__file__).resolve().parents[2]
class VanillaTitleBossBarCoverage(unittest.TestCase):
 def test_existing_central_title_subtitle_and_sao_boss_remain(self):
  d=json.loads((R/'extensions/modules/worlds-locations/module.json').read_text('utf8'))
  hooks={h['id']:h for h in d['hooks']}
  self.assertEqual((hooks['hud-location-title']['class'],hooks['hud-location-title']['method']),
                   ('net/minecraft/class_329','method_34004'))
  self.assertEqual((hooks['hud-location-subtitle']['class'],hooks['hud-location-subtitle']['method']),
                   ('net/minecraft/class_329','method_34002'))
  self.assertEqual(hooks['boss-plain']['class'],'fr/sao/saoui/mob/BossHud')
  self.assertEqual(hooks['boss-visible-text']['class'],'fr/sao/saoui/mob/BossHud')
 def test_native_bossbar_has_own_display_only_name_read_hook(self):
  hooks=json.loads((R/'extensions/modules/worlds-locations/module.json').read_text('utf8'))['hooks']
  h=next(h for h in hooks if h['id']=='vanilla-bossbar-title')
  self.assertEqual((h['class'],h['method'],h['descriptor']),
    ('net/minecraft/class_337','method_1796','(Lnet/minecraft/class_332;)V'))
  self.assertEqual((h['kind'],h['section'],h['member_class'],h['member'],h['member_descriptor'],h['sites']),
    ('call','names','net/minecraft/class_345','method_5414','()Lnet/minecraft/class_2561;',1))
  self.assertNotIn('capture',h)
 def test_seen_original_bossbar_labels_are_reviewed_once(self):
  d=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  for k,v in {'Ambush!':'伏擊!','Ambush! ':'伏擊! ',
              'Ambush — wave 1/3':'伏擊 — 第 1/3 波',
              'Ambush — wave 2/3':'伏擊 — 第 2/3 波',
              'Ambush — wave 3/3':'伏擊 — 第 3/3 波'}.items():
   self.assertEqual(d['names'].get(k),v,k)
   self.assertTrue(all(k not in d[s] for s in ('items','lines','messages')))
 def test_runtime_applies_native_styled_name_scoped_and_reuses_glossary(self):
  src=(R/'client-framework/src/sao/zh/engine')
  self.assertIn('SemanticText.boss(h)',(src/'RenderTextPipeline.java').read_text('utf8'))
  self.assertIn('SemanticText.boss(h)',(src/'SharedDictionary.java').read_text('utf8'))
  self.assertIn('static boolean boss(Script.Hook h)',(src/'SemanticText.java').read_text('utf8'))
  script=(src/'Script.java').read_text('utf8')
  self.assertIn('worlds-locations:vanilla-bossbar-title',script)
  self.assertIn('net/minecraft/class_345',script)
 def test_version_and_rule_total(self):
  ver=json.loads((R/'languages/zh_tw/zh-tw-version.json').read_text('utf8'))
  raw=(R/'languages/zh_tw/translations.json').read_bytes()
  self.assertEqual(ver['version'],'1.0.24')
  self.assertEqual(ver['translation']['size_bytes'],len(raw))
  self.assertEqual(ver['translation']['sha256'],hashlib.sha256(raw).hexdigest())
  self.assertEqual(sum(len(json.loads(raw)[s]) for s in ('names','items','lines','messages')),8885)
  sys.path.insert(0,str(R/'client-framework/tools'))
  from compose_localized import compose_bundle
  rules=compose_bundle(R,'zh_tw','1.15.0',119,int(time.time())+86400)
  self.assertEqual(len(rules['hooks']),137)
if __name__=='__main__':unittest.main()
