from pathlib import Path
import unittest,json,os
W=Path(__file__).parents[1];P=Path(os.environ.get('SAO_RULES_TEST',str(W.parent/'extensions/v2/rules-1.3.1.json')));B=json.loads(P.read_text('utf8'));H={h['id']:h for h in B['hooks']}
def render(id,s):
 if id not in H:raise AssertionError('Missing targeted hook '+id)
 h=H[id];value=h.get('exact',{}).get(s,s)
 for op in h.get('operations',[]):
  a,b=op['from'],op.get('to','')
  if op['op']=='replace_literal':value=value.replace(a,b)
  elif op['op']=='prefix' and value.startswith(a):value=b+value[len(a):]
  elif op['op']=='suffix' and value.endswith(a):value=value[:-len(a)]+b
 return value
class ProfileLobby(unittest.TestCase):
 def test_left_labels_native_spacing(self):
  for label,value in [('階級','TLZH'),('珂爾','3,761'),('遊玩','22 小時'),('等級','5'),('職業','刺客')]:
   with self.subTest(label=label):self.assertEqual(render('skills-progression:profile-left-label',label+'：')+' '+value,label+': '+value)
 def test_cached_left_row_spacing(self):
  for label in ['階級','珂爾','遊玩','等級','職業']:self.assertEqual(render('skills-progression:profile-left-cached',label+'： value'),label+': value')
 def test_right_colon_and_other_text_untouched(self):
  for text in ['生命值: 34.3','攻擊傷害: 1','提示：原樣保留','玩家名稱']:
   self.assertEqual(render('skills-progression:profile-left-cached',text),text)
  self.assertEqual(H['skills-progression:profile-left-cached']['descriptor'],'(Ljava/lang/String;FFFF)V')
 def test_repeat_has_no_double_space(self):
  for text in ['職業: 刺客','階級: TLZH']:self.assertEqual(render('skills-progression:profile-left-cached',text),text)
 def test_lobby_name(self):self.assertEqual(render('items-economy:native-item-name','Aincrad'),'艾恩葛朗特')
 def test_lobby_whole_lore(self):self.assertEqual(render('items-economy:item-lookup','Click to pick your floor'),'點擊選擇要前往的樓層')
 def test_lobby_colored_fragments(self):
  h='items-economy:item-lookup';self.assertEqual(render(h,'Click to pick your ')+render(h,'floor'),'點擊選擇要前往的樓層')
 def test_unknown_names_and_global_floor_not_changed(self):
  self.assertEqual(render('items-economy:native-item-name','My floor sword'),'My floor sword')
  self.assertEqual(render('dynamic-translation:names-string','floor'),'floor')
if __name__=='__main__':unittest.main()
