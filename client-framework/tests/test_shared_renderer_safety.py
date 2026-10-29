from pathlib import Path
import sys,json,time,unittest
W=Path(__file__).resolve().parents[1]
if not (W/'extensions/modules').is_dir():W=Path(__file__).resolve().parents[2]
tools=W/'tools' if (W/'tools/compose_localized.py').is_file() else W/'client-framework/tools'
sys.path.insert(0,str(tools))
from compose_localized import compose_bundle
class SharedRendererSafety(unittest.TestCase):
 def test_generic_renderer_only_uses_messages_not_player_name_dictionary(self):
  bundle=compose_bundle(W,'zh_tw','1.5.1',107,int(time.time())+86400)
  generic=[h for h in bundle['hooks'] if h['id'].startswith('ui-text-layout:canvas-') and h['id'] not in ('ui-text-layout:canvas-wrap-source',)]
  generic += [h for h in bundle['hooks'] if h['id'].startswith('ui-text-layout:util-')]
  self.assertGreaterEqual(len(generic),12)
  for h in generic:self.assertEqual(h['section'],'messages',h['id'])
 def test_user_names_never_in_shared_message_dictionary(self):
  data=json.loads((W/'languages/zh_tw/translations.json').read_text('utf8'))
  for text in ['Leader','Assassin','Aincrad','Pumba','Starting Town','TRAINING PAVISE']:
   self.assertNotIn(text,data['messages'],text)
if __name__=='__main__':unittest.main()
