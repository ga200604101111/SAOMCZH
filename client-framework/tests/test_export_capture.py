from pathlib import Path
import tempfile,json,sys,unittest
W=Path(__file__).resolve().parents[1]
if not (W/'extensions/modules').is_dir():W=W.parent
sys.path.insert(0,str(W/'tools'))
from export_inventory import Builder
class OptionalHookCaptureInventory(unittest.TestCase):
 def test_domain_capture_added_and_chat_rejected(self):
  with tempfile.TemporaryDirectory(prefix='sao-source-review-') as temp:
   root=Path(temp);folder=root/'SAO_TW_Dynamic/review';folder.mkdir(parents=True)
   cases=[
    {'schema_version':1,'locale':'zh_tw','origin':'hook.quest','section':'names','source_key':'The Chamber Beneath the Fields','status':'unreviewed'},
    {'schema_version':1,'locale':'zh_tw','origin':'hook.codex','section':'lines','source_key':'An unknown cave of many mysteries.','status':'unreviewed'},
    {'schema_version':1,'locale':'zh_tw','origin':'hook.skill','section':'lines','source_key':'恢復 40 Mana.','status':'unreviewed'},
    {'schema_version':1,'locale':'zh_tw','origin':'hook.chat','section':'lines','source_key':'Private Message: hello','status':'unreviewed'},
    {'schema_version':1,'locale':'zh_tw','origin':'hook.quest','section':'lines','source_key':'Player_123: hello','status':'unreviewed'},
    {'schema_version':1,'locale':'zh_tw','origin':'hook.quest','section':'lines','source_key':'www.example.com','status':'unreviewed'},
   ]
   (folder/'hook_missing_zh_tw.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in cases),'utf8')
   b=Builder(root);b.observations()
   self.assertIn(('names','The Chamber Beneath the Fields'),b.rows)
   self.assertIn(('lines','An unknown cave of many mysteries.'),b.rows)
   self.assertIn(('lines','恢復 40 Mana.'),b.rows)
   self.assertNotIn(('lines','Private Message: hello'),b.rows)
   self.assertNotIn(('lines','Player_123: hello'),b.rows)
   self.assertNotIn(('lines','www.example.com'),b.rows)
   q=[x for x in b.sources if x['source']=='review/hook_missing_zh_tw.jsonl']
   self.assertEqual(len(q),1)
   self.assertEqual(q[0]['records'],3)
if __name__=='__main__':unittest.main()
