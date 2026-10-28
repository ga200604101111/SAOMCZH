import json,sys,tempfile,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from export_missing import export,stable,safe
class ItemMissingTests(unittest.TestCase):
 def test_item_labels_and_lore_can_be_reviewed(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);folder=root/'SAO_TW_Dynamic';(folder/'review').mkdir(parents=True)
   (folder/'zh_tw.json').write_text(json.dumps({'items':{},'names':{'Known Blade':'已知之刃'},'lines':{}}),encoding='utf8')
   rows=[]
   for text,origin in [('Unknown Elixir','item_name'),('Recover some stamina.','item_lore'),('Known Blade','item_name'),('PlayerOne: hello','item_lore')]:
    rows.append({'locale':'zh_tw','section':'items','source_key':text,'origin':origin,'id':stable('items',text)})
   (folder/'review/missing_zh_tw.jsonl').write_text('\n'.join(json.dumps(r) for r in rows)+'\n',encoding='utf8')
   output,count,ignored=export(root);self.assertEqual(count,2)
   data=json.loads(output.read_text('utf8'));self.assertEqual({e['source_key'] for e in data['entries']},{'Unknown Elixir','Recover some stamina.'})
 def test_chat_never_added_to_item_allowlist(self):
  self.assertFalse(safe('Hello everyone','chat'));self.assertFalse(safe('192.168.1.1','item_lore'));self.assertFalse(safe('user@example.com','item_name'))
if __name__=='__main__':unittest.main()
