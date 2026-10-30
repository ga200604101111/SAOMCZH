from pathlib import Path
import json,os,sys,time,unittest,zipfile
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'client-framework/tools'))
from compose_localized import compose_bundle

def strings(value):
 if isinstance(value,str):yield value
 elif isinstance(value,dict):
  for v in value.values():yield from strings(v)
 elif isinstance(value,list):
  for v in value:yield from strings(v)

class HalfwidthColons(unittest.TestCase):
 def test_all_translation_values_use_ascii_colons(self):
  data=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  bad=[(s,k) for s in ('names','items','lines','messages') for k,v in data[s].items() if '\uff1a' in v]
  self.assertEqual(bad,[],f'Fullwidth colon remains in {len(bad)} translated values')
 def test_generated_hook_output_is_halfwidth(self):
  bundle=compose_bundle(ROOT,'zh_tw','1.7.0',109,int(time.time())+86400)
  bad=[h['id'] for h in bundle['hooks'] if any('\uff1a' in v for v in h.get('exact',{}).values()) or any('\uff1a' in o.get('to','') for o in h.get('operations',[]))]
  self.assertEqual(bad,[])
 def test_original_input_keys_are_not_normalized(self):
  data=json.loads((ROOT/'languages/zh_tw/translations.json').read_text('utf8'))
  import hashlib
  # Preserve every original matcher exactly; only exclude this explicitly reviewed addition batch.
  added={'names':{'Mini Treant','Treant','Local Lumberjack','Oak Stump','OAK STUMP','Marécage Putride'},
         'messages':{'Calme Mortel activé !','Calme Mortel déclenché !','Calme Mortel épuisé !','Calme Mortel prêt !','You are at the spawn.'}}
  reviewed=json.loads((ROOT/'client-framework/tests/reference/items-batch-20261030.json').read_text('utf8'))
  added['items']=set(reviewed['entries'])
  screenshot=json.loads((ROOT/'client-framework/tests/reference/screenshot-messages-20261030.json').read_text('utf8'))
  for sec,entries in screenshot.items():added.setdefault(sec,set()).update(entries)
  lore=json.loads((ROOT/'client-framework/tests/reference/lore-paragraphs-20261030.json').read_text('utf8'))
  for sec,entries in lore.items():added.setdefault(sec,set()).update(entries)
  surfaces=json.loads((ROOT/'client-framework/tests/reference/cross-surface-20261030.json').read_text('utf8'))
  for section,entries in surfaces.items():added.setdefault(section,set()).update(entries)
  for section,keys in added.items():self.assertTrue(keys<=set(data[section]),'Reviewed semantic names missing')
  digest=hashlib.sha256(json.dumps({s:sorted(set(data[s])-added.get(s,set())) for s in ('names','items','lines','messages')},ensure_ascii=False,sort_keys=True,separators=(',',':')).encode('utf8')).hexdigest()
  self.assertEqual(digest,'bce31ca40d8c8d7a9248b6d542871154fc7bc62dd65329546783d578d3970dbd','Original matching keys changed')
 @unittest.skipUnless(os.environ.get('SAO_GLYPH_PACK'),'External pack not supplied')
 def test_localized_pack_values_use_ascii_colons(self):
  with zipfile.ZipFile(os.environ['SAO_GLYPH_PACK']) as z:
   bad=[n for n in z.namelist() if '/lang/' in n and n.endswith(('/zh_tw.json','/zh_hk.json')) and any('\uff1a' in v for v in strings(json.loads(z.read(n))))]
  self.assertEqual(bad,[])

if __name__=='__main__':unittest.main()
