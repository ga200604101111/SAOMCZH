from __future__ import annotations
import hashlib,json,sys,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from github_sync import sync,checked_repo
from validate import ValidationError,validate_repo
def payload(obj):return (json.dumps(obj,ensure_ascii=False)+'\n').encode()
class TranslationSyncTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory()
  self.addCleanup(self.tmp.cleanup)
  self.root=Path(self.tmp.name)
  data={'schema_version':1,'locale':'zh_tw','names':{'Corrupted Boar':'腐化野豬','My title':'自己的標題'},'messages':{},'lines':{},'items':{}}
  folder=self.root/'SAO_TW_Dynamic'
  folder.mkdir()
  (folder/'zh_tw.json').write_bytes(payload(data))
  self.data={'schema_version':1,'locale':'zh_tw','fallback':'en_us','names':{'Sanglier corrompu':'腐化野豬','Corrupted Boar':'腐化野豬','My title':'GitHub標題'},'messages':{},'lines':{'Time: %s':'時間：%s'},'items':{}}
  self.raw=payload(self.data)
  self.version={'schema_version':1,'locale':'zh_tw','version':'1.0.0','min_client_protocol':1,
      'translation':{'path':'languages/zh_tw/translations.json','sha256':hashlib.sha256(self.raw).hexdigest(),'size_bytes':len(self.raw)}}
  self.index={'schema_version':1,'client_protocol':1,'locales':{'zh_tw':{'name':'繁體中文','version_manifest':'languages/zh_tw/zh-tw-version.json'}}}
  self.resources={'manifest.json':payload(self.index),'languages/zh_tw/zh-tw-version.json':payload(self.version),'languages/zh_tw/translations.json':self.raw}
  self.config={'enabled':True,'owner':'public-user','repo':'sao-l10n','ref':'main','locale':'zh_tw','apply_to_v9':False}
 def fetch(self,path,limit):
  self.assertIn(path,self.resources);self.assertLessEqual(len(self.resources[path]),limit)
  return self.resources[path]
 def test_public_repo_manifest_matches_export(self):
  report=validate_repo(ROOT)
  self.assertGreaterEqual(report['zh_tw']['entries'],373)
 def test_sync_caches_but_does_not_modify_live_v9_by_default(self):
  file=self.root/'SAO_TW_Dynamic/zh_tw.json'
  before=file.read_bytes()
  state=sync(self.root,self.config,self.fetch)
  self.assertEqual(state['entries'],4)
  self.assertFalse(state['applied_v9'])
  self.assertEqual(file.read_bytes(),before)
  self.assertEqual((self.root/'SAO_TW_Dynamic/github-cache/zh_tw.json').read_bytes(),self.raw)
 def test_apply_v9_with_backup_and_local_override(self):
  folder=self.root/'SAO_TW_Dynamic'
  (folder/'local-overrides.json').write_bytes(payload({'locale':'zh_tw','names':{'My title':'使用者覆寫'}}))
  cfg=dict(self.config,apply_to_v9=True)
  state=sync(self.root,cfg,self.fetch)
  result=json.loads((folder/'zh_tw.json').read_text('utf8'))
  self.assertTrue(state['applied_v9'])
  self.assertEqual(result['names']['My title'],'使用者覆寫')
  self.assertEqual(result['names']['Sanglier corrompu'],'腐化野豬')
  self.assertEqual(json.loads(Path(state['backup']).read_text('utf8'))['names']['My title'],'自己的標題')
 def test_broken_checksum_never_updates_live_v9(self):
  live=self.root/'SAO_TW_Dynamic/zh_tw.json';before=live.read_bytes()
  self.version['translation']['sha256']='0'*64
  self.resources['languages/zh_tw/zh-tw-version.json']=payload(self.version)
  with self.assertRaises(ValidationError):sync(self.root,dict(self.config,apply_to_v9=True),self.fetch)
  self.assertEqual(live.read_bytes(),before)
 def test_path_traversal_rejected(self):
  self.index['locales']['zh_tw']['version_manifest']='languages/zh_tw/../../secrets.json'
  self.resources['manifest.json']=payload(self.index)
  with self.assertRaises(ValidationError):sync(self.root,self.config,self.fetch)
 def test_bad_format_never_overwrites(self):
  live=self.root/'SAO_TW_Dynamic/zh_tw.json';before=live.read_bytes()
  self.data['lines']['Time: %s']='時間'
  self.raw=payload(self.data)
  self.version['translation'].update({'sha256':hashlib.sha256(self.raw).hexdigest(),'size_bytes':len(self.raw)})
  self.resources['languages/zh_tw/zh-tw-version.json']=payload(self.version)
  self.resources['languages/zh_tw/translations.json']=self.raw
  with self.assertRaises(ValidationError):sync(self.root,dict(self.config,apply_to_v9=True),self.fetch)
  self.assertEqual(live.read_bytes(),before)
 def test_offline_disabled_safe(self):
  with self.assertRaises(ValidationError):sync(self.root,dict(self.config,enabled=False),self.fetch)
  with self.assertRaises(ValidationError):checked_repo(dict(self.config,owner='https://evil/'))
 def test_missing_locale_without_overwriting(self):
  cfg=dict(self.config,locale='ja_jp')
  with self.assertRaises(ValidationError):sync(self.root,cfg,self.fetch)
  self.assertFalse((self.root/'SAO_TW_Dynamic/github-cache/ja_jp.json').exists())
if __name__=='__main__':unittest.main()
