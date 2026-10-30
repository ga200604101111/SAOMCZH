from pathlib import Path
import os,subprocess,sys,hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[3]
P=ROOT/'client-framework/addons/zapaudio-webui'
OUT=Path(os.environ.get('SAO_WEBUI_BUILD',r'D:/翻譯/SAO_ZH_ZapAudio_20261031/build'))
OUT.mkdir(parents=True,exist_ok=True);classes=OUT/'classes';classes.mkdir(exist_ok=True)
upstream=Path(os.environ['APPDATA'])/'.Triggers Client/instances/Sword Art Online/mods/zap-audio-fabric-1.0.0.jar'
mixin=next((Path(os.environ['APPDATA'])/'.Triggers Client/loader/libraries').rglob('sponge-mixin-*.jar'))
JDK=Path(r'D:/Program Files/Zulu/zulu-26/bin')
def sha(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for b in iter(lambda:f.read(4194304),b''):h.update(b)
 return h.hexdigest()
before=sha(upstream)
sources=sorted((P/'src').rglob('*.java'))
cmd=[str(JDK/'javac.exe'),'--release','21','-proc:none','-encoding','UTF-8','-cp',str(mixin),'-d',str(classes),*map(str,sources)]
x=subprocess.run(cmd,capture_output=True,timeout=60)
print('COMPILE',x.returncode,(x.stdout+x.stderr).decode('utf8','replace')[-1500:])
if x.returncode:raise SystemExit(x.returncode)
art=OUT/'SAOMCZH-ZapAudio-ZH-TW-1.0.0.jar'
with zipfile.ZipFile(art,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=8) as z:
 for f in sorted(classes.rglob('*.class')):z.write(f,f.relative_to(classes).as_posix())
 for f in (P/'fabric.mod.json',P/'saomczh-zapaudio.mixins.json'):z.write(f,f.name)
 for f in sorted((P/'assets').rglob('*')):
  if f.is_file():z.write(f,f.relative_to(P).as_posix())
with zipfile.ZipFile(art) as z:
 assert z.testzip() is None
 names=z.namelist()
 assert len(names)==len(set(names))
 for n in ('fabric.mod.json','saomczh-zapaudio.mixins.json','assets/saomczh_zapaudio/web/saomczh-zh-tw.js',
  'assets/zapaudioclient/lang/zh_tw.json','sao/zh/zapaudio/WebUiLocaleInstaller.class','sao/zh/zapaudio/mixin/WebClientExtractorMixin.class'):
  assert n in names,n
 assert not any(x.startswith('web/index') or x.endswith('voice-client.js') or x.endswith('music-player.js') or x.endswith('ambiance-player.js') for x in names)
 assert json.loads(z.read('fabric.mod.json'))['id']=='saomczh_zapaudio'
 assert json.loads(z.read('saomczh-zapaudio.mixins.json'))['client']==['WebClientExtractorMixin']
assert sha(upstream)==before
contract=P/'WebUiLocaleInstallerContract.java'
cmd=[str(JDK/'javac.exe'),'--release','21','-proc:none','-encoding','UTF-8','-cp',str(art),'-d',str(classes),str(contract)]
x=subprocess.run(cmd,capture_output=True,timeout=40)
print('TEST_COMPILE',x.returncode,(x.stdout+x.stderr).decode('utf8','replace')[-1200:])
if x.returncode:raise SystemExit(x.returncode)
java=Path(os.environ['APPDATA'])/'.Triggers Client/runtime/jre-21.0.7-windows-x64/bin/java.exe'
cmd=[str(java),'-Xverify:all','-Dfile.encoding=UTF-8','-cp',os.pathsep.join([str(classes),str(art)]),'sao.zh.zapaudio.WebUiLocaleInstallerContract',str(upstream)]
x=subprocess.run(cmd,capture_output=True,timeout=60)
print('INSTALLER_CONTRACT',x.returncode,(x.stdout+x.stderr).decode('utf8','replace')[-1800:])
if x.returncode:raise SystemExit(x.returncode)
report={'artifact':str(art),'sha256':sha(art),'size_bytes':art.stat().st_size,'original_jar_sha256':before,'original_jar_untouched':True,'html_source_hash_pinned':True,'media_js_unmodified':True,'test':x.stdout.decode('utf8','replace').strip()}
(OUT/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n','utf8')
print('ZAPAUDIO_ADDON_BUILD_PASS',json.dumps(report,ensure_ascii=False))
