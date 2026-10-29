from pathlib import Path
import json,unittest,os,re
W=Path(__file__).parents[1];P=Path(os.environ.get('SAO_RULES_TEST',str(W.parent/'SAO_TW_GitHub_Export/extensions/v2/rules-1.2.0.json')))
B=json.loads(P.read_text('utf8'));H={h['id']:h for h in B['hooks']}
def render(id,s):
 if id not in H:raise AssertionError('Missing dedicated display hook: '+id)
 h=H[id];v=h.get('exact',{}).get(s,s)
 for o in h.get('operations',[]):
  a=o['from'];b=o.get('to','')
  if o['op']=='replace_literal':v=v.replace(a,b)
  elif o['op']=='prefix' and v.startswith(a):v=b+v[len(a):]
  elif o['op']=='suffix' and v.endswith(a):v=v[:-len(a)]+b
 return v
class Screenshots(unittest.TestCase):
 def test_damage_values_and_icon_preserved(self):
  self.assertEqual(render('combat-feedback:damage-segment','⚔ 打擊 21 | total 1009 | ~26 DPS'),'⚔ 單次傷害 21 | 總傷害 1009 | 約 26 DPS')
 def test_damage_fragmented_and_idempotent(self):
  for a,b in [('Coup','單次傷害'),('total','總傷害'),('單次傷害','單次傷害'),('⚔ 單次傷害 21 | 總傷害 1009 | 約 26 DPS','⚔ 單次傷害 21 | 總傷害 1009 | 約 26 DPS')]:self.assertEqual(render('combat-feedback:damage-segment',a),b)
 def test_healing_not_mislabeled_as_damage(self):self.assertEqual(render('combat-feedback:damage-segment','total soigné 548'),'總治療量 548')
 def test_item_names(self):
  for a,b in [('Training Pavise','訓練用大盾'),('Training Katana','訓練用武士刀'),('Stamina Shard','耐力碎片')]:self.assertEqual(render('items-economy:item-plain',a),b)
 def test_item_descriptions(self):
  self.assertEqual(render('items-economy:tooltip-display','An old shield.'),'一面老舊的盾牌。')
  self.assertEqual(render('items-economy:tooltip-display','its great weight'),'它十分沉重。')
 def test_item_properties_keep_numbers(self):self.assertEqual(render('items-economy:tooltip-display','Ꭱ Guard Durability: 8'),'Ꭱ 格擋耐久： 8')
 def test_guild_rank_only(self):
  for a,b in [('Leader','會長'),('Captain','隊長'),('Member','成員')]:self.assertEqual(render('guild-social:member-rank',a),b)
  h=H.get('guild-social:member-rank',{});self.assertEqual(h.get('member'),'rank');self.assertNotEqual(h.get('member'),'name')
 def test_profile_class(self):self.assertEqual(render('skills-progression:profile-class','Assassin'),'刺客')
 def test_world_and_seconds_scoped(self):
  self.assertEqual(render('worlds-locations:server-title','傳送→FRACTURED UNDERWORLD'),'傳送→破碎冥界')
  self.assertEqual(render('worlds-locations:transfer-duration','5s'),'5 秒')
 def test_location_confirmation(self):self.assertEqual(render('worlds-locations:point-confirm','Jardin des Géants'),'巨人庭園')
 def test_quest_title_description_objective(self):
  for a,b in [('19. The Chamber Beneath the Fields','19. 田野下的密室'),('Go to where the bandits live','前往盜匪的居所')]:self.assertEqual(render('quests-objectives:description-text',a),b)
 def test_codex_region(self):self.assertEqual(render('codex-creatures:entry-name','Starting Town'),'起始之城')
 def test_compass(self):
  self.assertEqual(render('worlds-locations:compass-title','Starting Town'),'起始之城')
  self.assertEqual(render('worlds-locations:compass-objective','Se rendre là où résident les brigands'),'前往盜匪的居所')
 def test_requested_projectile_term(self):self.assertEqual(render('items-economy:equipment-stat','Projectile dmg.'),'投射傷害')
 def test_unknown_player_text_not_filtered(self):
  for h in B['hooks']:self.assertNotIn(h['class'],['net/minecraft/class_338','net/minecraft/class_634'])
  self.assertEqual(render('combat-feedback:damage-segment','null'),'null');self.assertEqual(render('combat-feedback:damage-segment','old'),'old')
if __name__=='__main__':unittest.main()
