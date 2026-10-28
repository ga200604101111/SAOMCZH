from __future__ import annotations
import hashlib,json,sys,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/"tools"))
from export_missing import export,safe
class MissingExports(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
  self.root=Path(self.tmp.name);self.folder=self.root/"SAO_TW_Dynamic"
  (self.folder/"review").mkdir(parents=True)
  (self.folder/"zh_tw.json").write_text(json.dumps({"schema_version":1,"locale":"zh_tw",
       "names":{"Already Known":"已翻譯"},"messages":{},"lines":{},"items":{}}),encoding="utf8")
 def observation(self,section,source,origin):
  return {"schema_version":1,"locale":"zh_tw","section":section,"source_key":source,"origin":origin,
          "id":hashlib.sha256((section+"\0"+source).encode()).hexdigest()[:24],"status":"unreviewed","zh_tw":""}
 def test_observations_are_filtered(self):
  rows=[
   self.observation("names","Attack damage","equipment_stat"),
   self.observation("names","Attack damage","equipment_stat"),
   self.observation("names","Already Known","equipment_stat"),
   self.observation("lines","Restore %s stamina","skill_desc"),
   self.observation("lines","PlayerOne: hello","skill_desc"),
   self.observation("names","Sanglier Corrompu","mob_name"),
   self.observation("names","YingHuang09","mob_name"),
   self.observation("messages","Hello everyone","chat"),
   self.observation("names","192.168.1.100","mob_name"),
   self.observation("names","Visit https://example.org","bossbar_title"),
   self.observation("lines","你好","skill_desc")
  ]
  file=self.folder/"review/missing_zh_tw.jsonl"
  file.write_text("".join(json.dumps(v,ensure_ascii=False)+"\n" for v in rows),encoding="utf8")
  path,n,ignored=export(self.root)
  self.assertEqual(n,3);self.assertGreaterEqual(ignored,6)
  package=json.loads(path.read_text("utf8"))
  self.assertEqual(package["type"],"missing-translation-proposals")
  self.assertEqual(len(package["entries"]),3)
  self.assertTrue(all("username" not in str(v) for v in package["entries"]))
  self.assertTrue(all("chat" not in str(v) for v in package["entries"]))
  self.assertIn("Restore %s stamina",[e["source_key"] for e in package["entries"]])
  self.assertNotIn("Already Known",[e["source_key"] for e in package["entries"]])
 def test_empty_missing_file(self):
  (self.folder/"review/missing_zh_tw.jsonl").write_text("",encoding="utf8")
  path,n,skipped=export(self.root)
  self.assertEqual(n,0)
  self.assertEqual(json.loads(path.read_text("utf8"))["entries"],[])
 def test_chat_and_personal_info_disallowed(self):
  for text in ["Hello: world","<Player> hello","player@domain.com","192.168.0.1",
               "123e4567-e89b-12d3-a456-426614174000","Hi » user","French description\u0000secret"]:
   if "\0" in text:continue
   self.assertFalse(safe(text,"chat"))
  self.assertFalse(safe("YingHuang09","mob_name"))
if __name__=="__main__":unittest.main()
