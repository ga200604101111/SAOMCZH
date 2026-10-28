#!/usr/bin/env python3
"""Export a deliberately privacy-filtered, deduplicated missing-translation submission. No upload."""
from __future__ import annotations
import argparse,datetime,hashlib,json,re,sys
from pathlib import Path
ALLOWED={
 "equipment_stat","skill_name","skill_desc","skill_stat","skill_lore","mob_name",
 "bossbar_title","scoreboard_title","actionbar_system","item_name","item_lore"
}
SECTIONS={"names","messages","lines","items"}
UUID=re.compile(r"\b[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}\b",re.I)
IP=re.compile(r"\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b")
PLAYER=re.compile(r"^\s*(?:\[[^]]{1,25}\])?\s*[a-zA-Z0-9_]{3,16}\s*[:：»›].*",re.S)
TOKEN=re.compile(r"%(?:[0-9]+\$)?[-+#0 ,(]*[0-9]*(?:\.[0-9]+)?[a-zA-Z]|%%|\{[^{}\r\n]{1,80}\}")
def safe(text:str,origin:str)->bool:
 if not isinstance(text,str) or origin not in ALLOWED or not 3<=len(text)<=480:return False
 if not any(c.isalpha() for c in text):return False
 if UUID.search(text) or IP.search(text) or PLAYER.match(text):return False
 if any(c in text for c in ("@","§","»","‹","<",">","\\")):return False
 if "://" in text or re.search(r"[\u3400-\u9fff]",text):return False
 if origin=="mob_name" and not re.search(r"\s",text):return False
 return True
def stable(section:str,source:str)->str:
 return hashlib.sha256((section+"\x00"+source).encode("utf-8")).hexdigest()[:24]
def export(root:Path)->tuple[Path,int,int]:
 source=root/"SAO_TW_Dynamic/review/missing_zh_tw.jsonl"
 if not source.is_file():raise FileNotFoundError(f"No missing translations yet: {source}")
 if source.stat().st_size>12*1024*1024:raise ValueError("Missing translations exceed safe export size")
 translated=root/"SAO_TW_Dynamic/zh_tw.json"
 existing=json.loads(translated.read_text("utf8")) if translated.exists() else {}
 found={};ignored=0
 with source.open(encoding="utf-8") as src:
  for idx,line in enumerate(src,1):
   if idx>30000:raise ValueError("Exceeded maximum observation count")
   try:row=json.loads(line)
   except ValueError:
    ignored+=1
    continue
   origin=row.get("origin");section=row.get("section");text=row.get("source_key")
   if section not in SECTIONS or not safe(text,origin) or row.get("locale")!="zh_tw":
    ignored+=1;continue
   if row.get("id")!=stable(section,text):
    ignored+=1;continue
   sections=("items","names","lines") if section=="items" else (section,)
   if any(text in existing.get(part,{}) for part in sections):
    ignored+=1;continue
   key=(section,text)
   if key not in found:
    found[key]={
     "section":section,
     "source_key":text,
     "source_language":"undetermined",
     "suggested_zh_tw":"",
     "status":"needs-review",
     "placeholders":sorted(TOKEN.findall(text))
    }
 entries=sorted(found.values(),key=lambda v:(v["section"],v["source_key"]))
 package={
  "schema_version":1,"locale":"zh_tw","type":"missing-translation-proposals",
  "generated_at":datetime.datetime.now(datetime.timezone.utc).isoformat(),
  "privacy":"Only allowlisted UI/item text is included, with common identifier filters. No chat event or raw packet capture. Manually review custom item names before sharing.",
  "entries":entries
 }
 folder=root/"SAO_TW_Dynamic/submissions";folder.mkdir(exist_ok=True)
 dest=folder/("missing_zh_tw_"+datetime.datetime.now().strftime("%Y%m%d_%H%M%S")+".json")
 if dest.exists():raise FileExistsError("Refusing to overwrite previous submission")
 dest.write_text(json.dumps(package,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
 return dest,len(entries),ignored
def main():
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument("--root",type=Path,default=Path("D:/翻譯"))
 args=p.parse_args()
 try:
  target,count,skipped=export(args.root)
  print(f"MISSING_SUBMISSION_READY path={target} entries={count} skipped={skipped} (not uploaded)")
 except (ValueError,FileNotFoundError,OSError) as e:
  print(f"MISSING_SUBMISSION_ERROR {e}",file=sys.stderr);return 1
 return 0
if __name__=="__main__":sys.exit(main())
