from pathlib import Path
import tempfile,json,unittest,sys,zipfile,hashlib
W=Path(__file__).parents[1];sys.path.insert(0,str(W/'tools'))
try:import export_inventory as tool
except ImportError:tool=None
class InventoryTests(unittest.TestCase):
 def setUp(self):
  self.assertIsNotNone(tool,'All-source inventory exporter has not been implemented')
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name);self.d=self.root/'SAO_TW_Dynamic';(self.d/'raw').mkdir(parents=True);(self.d/'review').mkdir()
  self.write(self.d/'zh_tw.json',{'names':{},'items':{},'lines':{},'messages':{}})
 def write(self,p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False)+'\n','utf8')
 def table(self,name='en_a.json',date='2026-10-29T01:00:00Z',**sections):self.write(self.d/'raw'/name,{'origin':'SAOUI.LangTable','served_language':'en','captured_at':date,'sections':sections})
 def obs(self,text,origin='item_lore',section='items',filename='missing_zh_tw.jsonl'):
  with (self.d/'review'/filename).open('a',encoding='utf8') as f:f.write(json.dumps({'source_key':text,'origin':origin,'section':section},ensure_ascii=False)+'\n')
 def run_inventory(self,game=None):return tool.collect(self.root,game)
 def test_unopened_table_collected(self):
  self.table(names={'Une quête':'An unopened quest'},lines={'Description':'A long unseen quest description'});r=self.run_inventory();self.assertEqual(len(r['entries']),2);self.assertTrue(all(x['status']=='missing' for x in r['entries']))
 def test_mixed_language_not_dropped(self):
  self.obs('恢復 40 Mana');x=self.run_inventory()['entries'][0];self.assertEqual(x['status'],'partial');self.assertEqual(x['text'],'恢復 40 Mana')
 def test_colour_and_format_preserved(self):
  self.obs('§cStay out of combat for 3 seconds');x=self.run_inventory()['entries'][0];self.assertIn('§cStay out of combat for 3 seconds',x['variants']);self.assertEqual(x['text'],'Stay out of combat for 3 seconds')
 def test_long_description_and_placeholders(self):
  text='Description '+('long sentence '*100)+' %s {count}';self.table(lines={'Texte':text});x=self.run_inventory()['entries'][0];self.assertEqual(x['text'],tool.normalize(text));self.assertIn(text,x['variants']);self.assertEqual(x['placeholders'],['%s','{count}'])
 def test_all_explicit_observation_sources(self):
  for name,origin in [('item_seen.jsonl','SAOUI.ItemLangClient'),('ui_seen.jsonl','saojob.MapNode'),('daily_seen.jsonl','SAOUI.DailyModel.Reward.desc')]:self.obs('Example '+name,origin=origin,filename=name)
  self.assertEqual(len(self.run_inventory()['entries']),3)
 def test_snapshot_dedup_and_revisions(self):
  self.table('en_a.json','2026-10-28T01:00:00Z',names={'Bonjour':'Hello','Retired':'Old content'});self.table('en_b.json','2026-10-29T01:00:00Z',names={'Bonjour':'Hello','New':'New content'});r=self.run_inventory();self.assertEqual(len(r['entries']),3);self.assertEqual(sum(x['historical_only'] for x in r['entries']),1)
 def test_existing_translation_not_called_rendered(self):
  self.table(names={'Bonjour':'Hello'});self.write(self.d/'zh_tw.json',{'names':{'Hello':'你好'}});x=self.run_inventory()['entries'][0];self.assertEqual(x['status'],'mapped-needs-display-check');self.assertNotIn('rendered',x)
 def test_french_alias_lookup(self):
  self.table(names={'Bonjour':'Hello'});self.write(self.d/'zh_tw.json',{'names':{'Bonjour':'你好'}});self.assertEqual(self.run_inventory()['entries'][0]['translations'],['你好'])
 def test_conflicting_aliases(self):
  self.table(names={'Bonjour':'Hello'});self.write(self.d/'zh_tw.json',{'names':{'Bonjour':'您好','Hello':'你好'}});self.assertEqual(self.run_inventory()['entries'][0]['status'],'conflicting-mappings')
 def test_latest_override_wins(self):
  self.obs('Healing Shard');self.write(self.d/'zh_tw.json',{'items':{'Healing Shard':'舊譯'}});self.write(self.d/'local-overrides.json',{'items':{'Healing Shard':'治療碎片'}});self.assertEqual(self.run_inventory()['entries'][0]['translations'],['治療碎片'])
 def test_private_origins_and_identifiers_excluded(self):
  for text,origin in [('Player: a private message','player_chat'),('pm user@example.com','item_lore'),('secret 192.168.1.2','item_lore'),('http://private.example/token','item_lore')]:self.obs(text,origin)
  self.obs('Class: warrior');r=self.run_inventory();self.assertEqual(len(r['entries']),1);self.assertEqual(r['entries'][0]['text'],'Class: warrior');self.assertTrue(r['summary']['filtered'])
 def test_malformed_trailing_observation_retains_prior(self):
  self.obs('Mana Shard');p=self.d/'review/missing_zh_tw.jsonl';p.write_text(p.read_text('utf8')+'{"partial":','utf8');r=self.run_inventory();self.assertEqual(len(r['entries']),1);self.assertTrue(r['summary']['warnings'])
 def test_numbers_grouped_without_rewriting_raw(self):
  self.obs('Minimum Level: 1');self.obs('Minimum Level: 5');r=self.run_inventory();self.assertEqual(len(r['entries']),2);self.assertEqual(len(r['groups']),1);self.assertEqual(r['groups'][0]['count'],2)
 def test_static_lang_assets_and_chinese_value(self):
  g=self.root/'game';(g/'mods').mkdir(parents=True)
  with zipfile.ZipFile(g/'mods/testmod.jar','w') as z:
   z.writestr('assets/test/lang/en_us.json',json.dumps({'ui.title':'Quest Log','ui.other':'Untranslated Button'}));z.writestr('assets/test/lang/zh_tw.json',json.dumps({'ui.title':'任務日誌'}));z.writestr('assets/test/secrets.json','{"password":"do not read"}')
  r=self.run_inventory(g);self.assertEqual(len(r['entries']),2);self.assertEqual(next(x for x in r['entries'] if x['text']=='Quest Log')['status'],'mapped-needs-display-check')
 def test_duplicate_json_keys_rejected(self):
  (self.d/'raw/duplicate.json').write_text('{"origin":"SAOUI.LangTable","sections":{},"sections":{}}','utf8');self.assertTrue(self.run_inventory()['summary']['warnings'])
 def test_never_reads_chat_log(self):
  (self.d/'review/private_chat.jsonl').write_text('{"source_key":"Private user phrase","origin":"item_lore","section":"items"}\n','utf8');self.assertEqual(self.run_inventory()['entries'],[])
 def test_atomic_export_reusable(self):
  self.table(names={'Bonjour':'Hello'});first=tool.export(self.root);second=tool.export(self.root);self.assertEqual(first,second);self.assertTrue((first/'all_text.jsonl').exists());self.assertTrue((first/'missing_text.jsonl').exists())
if __name__=='__main__':unittest.main()
