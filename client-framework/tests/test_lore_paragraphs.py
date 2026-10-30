from pathlib import Path
import json,sys,time,unittest
R=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(R/'client-framework/tools'))
from compose_modules import checked_hook,ModuleError
from compose_localized import compose_bundle

class LoreParagraphs(unittest.TestCase):
 def test_description_list_has_a_precise_bounded_hook(self):
  source={'id':'tooltip-description-paragraphs','class':'fr/sao/tooltip/TipModel','method':'description','descriptor':'()Ljava/util/List;','kind':'description_list','section':'items','capture':'item'}
  hook=checked_hook(source,'items-economy',{})
  self.assertEqual(hook['kind'],'description_list')
  for change in [{'class':'net/minecraft/class_338'},{'method':'effects'},{'section':'messages'},{'descriptor':'()Ljava/util/Collection;'}]:
   with self.subTest(change=change):
    with self.assertRaises(ModuleError):checked_hook(dict(source,**change),'items-economy',{})
 def test_hook_is_installed_not_generic_return(self):
  bundle=compose_bundle(R,'zh_tw','1.9.0',113,int(time.time())+86400)
  hooks={h['id']:h for h in bundle['hooks']}
  self.assertEqual(hooks['items-economy:tooltip-description-paragraphs']['kind'],'description_list')
 def test_full_description_and_floating_text_added(self):
  d=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  source='A material of nature,\nused mainly\nfor light and fast\narmor.'
  self.assertEqual(d['items'].get(source),'一種天然材料，主要用於製作輕巧靈活的護甲。')
  self.assertEqual(d['lines'].get('Impossible Jump and Run'),'不可能的跳躍跑酷')
  self.assertEqual(d['lines'].get('Respect if you finish it'),'成功通關者值得敬佩')
  self.assertNotIn('Impossible Jump and Run Lv. 1+',d['lines'])
 def test_original_fragment_translations_remain(self):
  d=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
  self.assertEqual(d['items']['used mainly'],'主要用於')
  self.assertEqual(d['items']['armor.'],'護甲。')

if __name__=='__main__':unittest.main()
