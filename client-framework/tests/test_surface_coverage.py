from pathlib import Path
import json,sys,time,unittest,collections
W=Path(__file__).resolve().parents[1]
if not (W/'extensions/modules').is_dir():W=W.parent
sys.path.insert(0,str(W/'tools' if (W/'tools/compose_localized.py').is_file() else W/'client-framework/tools'))
from compose_localized import compose_bundle
from test_central_locale import engine
def bundle():return compose_bundle(W,'zh_tw','1.6.0',108,int(time.time())+86400*100)
class DisplayExpansion(unittest.TestCase):
    def test_quest_row_and_journal_titles(self):
        b={x['id']:x for x in bundle()['hooks']}
        for key in ['quest-row-title','quest-row-status','quest-row-objective','quest-detail-heading']:
            self.assertIn('quests-objectives:'+key,b)
        self.assertEqual(b['quests-objectives:quest-row-title']['argument'],1)
        self.assertEqual(b['quests-objectives:quest-row-status']['argument'],2)
        self.assertEqual(b['quests-objectives:quest-row-objective']['argument'],3)
    def test_hud_quest_complete_caller_surfaces(self):
        ids={h['id'] for h in bundle()['hooks']}
        for k in ['hud-original-title','hud-original-objective','hud-objective-label',
                  'hud-tab-source','compass-visible-title','compass-top-line','compass-bottom-line']:
            self.assertIn('quests-objectives:'+k,ids)
    def test_codex_lore_hint_and_labels(self):
        h={x['id']:x for x in bundle()['hooks']}
        expected=['codex-group-name','codex-entry-hint','codex-body-hint',
                  'codex-display-hint','codex-display-lore','codex-category-display']
        for k in expected:self.assertIn('codex-creatures:'+k,h)
        self.assertEqual(h['codex-creatures:codex-body-hint']['sites'],3)
        self.assertEqual(h['codex-creatures:codex-display-hint']['sites'],2)
        self.assertEqual(h['codex-creatures:codex-display-lore']['sites'],1)
    def test_skill_entry_description_before_split(self):
        h={x['id']:x for x in bundle()['hooks']}
        self.assertIn('skills-progression:skill-description',h)
        self.assertEqual(h['skills-progression:skill-description']['sites'],3)
        self.assertIn('skills-progression:skilltree-description-before-split',h)
        self.assertIn('skills-progression:skilltree-line-text',h)
    def test_guild_detail_rank_only(self):
        h={x['id']:x for x in bundle()['hooks']}
        k=h['guild-social:member-detail-rank']
        self.assertEqual(k['kind'],'field')
        self.assertEqual(k['member'],'rank')
        self.assertNotEqual(k['member'],'name')
    def test_unchanged_previous_surfaces_and_bound(self):
        b=bundle();hooks={h['id']:h for h in b['hooks']}
        old=json.loads((W/'audit/rules-1.5.1.json' if (W/'audit/rules-1.5.1.json').is_file() else W/'extensions/v2/rules-1.5.1.json').read_text('utf8'))
        self.assertGreater(len(hooks),88)
        self.assertLessEqual(len(hooks),128)
        for prev in old['hooks']:
            self.assertIn(prev['id'],hooks)
            for k in ['class','method','descriptor','kind','argument','member_class','member','member_descriptor','sites','locale','section']:
                self.assertEqual(prev.get(k),hooks[prev['id']].get(k),prev['id'])
    def test_dedicated_translation_semantics_and_no_player_chat(self):
        b={x['id']:x for x in bundle()['hooks']}
        dictionary=json.loads((W/'languages/zh_tw/translations.json').read_text('utf8'))
        self.assertEqual(engine(b['codex-creatures:codex-group-name'],'Starting Town',dictionary),'起始之城')
        self.assertEqual(engine(b['quests-objectives:quest-row-title'],'19. The Chamber Beneath the Fields',dictionary),'19. 田野下的密室')
        self.assertEqual(engine(b['guild-social:member-detail-rank'],'Leader',dictionary),'會長')
        self.assertNotIn('net/minecraft/class_338',[v['class'] for v in b.values()])
        for text in ['YingHuang09','User_123: hello','null','old']:
            self.assertEqual(engine(b['quests-objectives:quest-row-title'],text,dictionary),text)
            self.assertEqual(engine(b['codex-creatures:codex-group-name'],text,dictionary),text)
    def test_modules_contain_no_translation_data(self):
        for p in (W/'extensions/modules').glob('*/module.json'):
            m=json.loads(p.read_text('utf8'))
            self.assertNotIn('dictionary',m)
            for h in m['hooks']:
                self.assertNotIn('exact',h)
                self.assertNotIn('operations',h)
                self.assertNotIn('locale',h)
if __name__=='__main__':unittest.main()
