'use strict';
// Execute the upstream bootstrap and SAOMCZH overlay in a simulated MCEF document.
// No external browser, Edge/Chromium automation, media device or Minecraft instance required.
const fs=require('node:fs'), vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync(process.argv[2],'utf8');
const locale=fs.readFileSync(process.argv[3],'utf8');
const match=html.match(/<script>\s*([\s\S]*?)<\/script>/);
assert.ok(match,'Upstream Sound of Aincrad bootstrap not found');
const bootstrap=match[1];
class El {
 constructor(value,attrs={}) {this.textContent=value;this.childElementCount=0;this.attrs={...attrs};this.onclick=()=>{};this.value='default';}
 getAttribute(n){return Object.hasOwn(this.attrs,n)?this.attrs[n]:null;}
 setAttribute(n,v){this.attrs[n]=v;}
}
let checks=0;
for(const mode of ['zh_tw','en_us','fr_fr']) {
 const e={
  brand:new El('Sound of Aincrad'),voice:new El('Voix'),nearby:new El('Joueurs à proximité'),
  settings:new El('Paramètres Audio'),output:new El('Sortie Audio'),
  title:new El('Refresh',{title:'Rafraîchir'}), tooltip:new El('?',{'data-tooltip':"C'est le minimum entre votre portée et celle de l'autre joueur qui s'applique. Si un joueur a une portée plus faible que la vôtre, vous ne l'entendrez pas (et lui non plus) au-delà de sa portée à lui."}),
  volumes:new El('Volumes'),microphone:new El('Microphone'),test:new El('Test'),ptt:new El('Appuyer pour parler'),
  key:new El('4'),player:new El('YingHuang09'),input:new El('麥克風 (HyperX Virtual Surround Sound)'),outputDevice:new El('喇叭 (HyperX Virtual Surround Sound)'),
 };
 let clickCount=0;e.voice.onclick=()=>clickCount++;
 const callbacks=[],append=[];
 const selectors={
  '[data-zap-brand]':[e.brand],
  '[data-i18n]':[e.voice,e.nearby,e.settings,e.output,e.ptt],
  '[data-i18n-title]':[e.title],
  '[data-i18n-tooltip]':[e.tooltip],
  'h3':[e.volumes,e.nearby],
  '#settingsModal label':[e.microphone],
  '#settingsModal span':[e.test,e.key,e.ptt]
 };
 const document={
  readyState:'loading',title:'Sound of Aincrad',
  documentElement:{lang:'fr'},
  head:{appendChild:x=>append.push(x)},
  createElement:tag=>({tag,style:{},id:'',textContent:''}),
  addEventListener(name,cb){assert.equal(name,'DOMContentLoaded');callbacks.push(cb);},
  querySelectorAll(sel){assert.ok(Object.hasOwn(selectors,sel),'Unauthorized DOM scan: '+sel);return selectors[sel];}
 };
 const window={location:{search:'?zapLang='+mode},ZAP_AUDIO_CONFIG:null};
 const sandbox={window,document,URLSearchParams,console};
 vm.runInNewContext(bootstrap,sandbox,{timeout:2000,filename:'upstream-index-template.html'});
 const oldT=window.ZAP_T;
 vm.runInNewContext(locale,sandbox,{timeout:2000,filename:'saomczh-zh-tw.js'});
 if(mode==='zh_tw') assert.equal(window.ZAP_BRAND_NAME,'艾恩葛朗特之聲','Brand must be zh_tw before the original voice client is evaluated');
 for(const fn of callbacks)fn();
 e.voice.onclick();
 assert.equal(clickCount,1);
 assert.equal(e.player.textContent,'YingHuang09');
 assert.equal(e.key.textContent,'4');
 assert.equal(e.input.textContent,'麥克風 (HyperX Virtual Surround Sound)');
 assert.equal(e.outputDevice.textContent,'喇叭 (HyperX Virtual Surround Sound)');
 assert.equal(e.input.value,'default');
 assert.equal(e.outputDevice.value,'default');
 checks+=7;
 if(mode==='zh_tw') {
  assert.notEqual(window.ZAP_T,oldT);
  assert.equal(e.brand.textContent,'艾恩葛朗特之聲');
  assert.equal(e.voice.textContent,'語音');
  assert.equal(e.nearby.textContent,'附近玩家');
  assert.equal(e.settings.textContent,'音訊設定');
  assert.equal(e.output.textContent,'音訊輸出');
  assert.equal(e.volumes.textContent,'音量');
  assert.equal(e.microphone.textContent,'麥克風');
  assert.equal(e.test.textContent,'測試');
  assert.equal(e.ptt.textContent,'按鍵說話');
  assert.equal(e.title.getAttribute('title'),'重新整理');
  assert.ok(e.tooltip.getAttribute('data-tooltip').startsWith('雙方的通話距離'));
  assert.equal(document.documentElement.lang,'zh-TW');
  assert.equal(window.ZAP_T('Connecté'),'已連線');
  assert.equal(window.ZAP_T('Vocal actif'),'語音已啟用');
  assert.equal(window.ZAP_T('Reconnexion (%1/%2)...',2,5),'正在重新連線（2/5）……');
  assert.equal(window.ZAP_T('Micro: %1','Razer BlackShark V3'),'麥克風: Razer BlackShark V3');
  assert.equal(window.ZAP_T('Raison: %1','test reason'),'原因: test reason');
  assert.equal(window.ZAP_T('Unknown dynamic source'),'Unknown dynamic source');
  assert.equal(append.length,1);
  checks+=19;
 } else {
  assert.equal(window.ZAP_T,oldT,'Non-Chinese locale must keep upstream i18n');
  assert.equal(append.length,0);
  assert.equal(e.brand.textContent,mode==='fr_fr'?"Son de l'Aincrad":'Sound of Aincrad');
  assert.equal(e.nearby.textContent,mode==='fr_fr'?'Joueurs à proximité':'Nearby players');
  assert.equal(window.ZAP_T('Connecté'),mode==='fr_fr'?'Connecté':'Connected');
  assert.equal(window.ZAP_T('Reconnexion (%1/%2)...',2,5),mode==='fr_fr'?'Reconnexion (2/5)...':'Reconnecting (2/5)...');
  checks+=6;
 }
 console.log('WEBUI_MCEF_DOCUMENT_VM_PASS',mode,'status=',window.ZAP_T('Connecté'),'device_name_unchanged=true','events_preserved=true');
}
console.log('WEBUI_MCEF_BOOTSTRAP_CONTRACT_PASS checks='+checks+' upstream_dom_bootstrap=true no_browser_launched=true');
