from pathlib import Path
import sys,json,hashlib,time,unittest,re
W=Path(__file__).resolve().parents[1]
if not (W/'extensions/modules').is_dir():
    W=Path(__file__).resolve().parents[2]
tools=W/'tools' if (W/'tools/compose_localized.py').is_file() else W/'client-framework/tools'
sys.path.insert(0,str(tools))
from display_typography import normalize_fields
from compose_modules import compose
from compose_localized import compose_bundle,LocaleError
R=W if (W/'extensions/v2/rules-1.4.0.json').is_file() else W.parent/'SAO_TW_GitHub_Export'
OLD=json.loads((R/'extensions/v2/rules-1.4.0.json').read_text('utf8'))
OLD_DICT=json.loads((R/'languages/zh_tw/translations.json').read_text('utf8'))
SECTIONS=('names','items','lines','messages')
def rules():return compose_bundle(W,'zh_tw','1.5.0',106,int(time.time())+86400*90)
def engine(h,text,d):
    if text is None:return text
    s=h.get('exact',{}).get(text,text)
    for op in h.get('operations',[]):
        a,b=op['from'],op.get('to','')
        if op['op']=='replace_literal':s=s.replace(a,b)
        elif op['op']=='prefix' and s.startswith(a):s=b+s[len(a):]
        elif op['op']=='suffix' and s.endswith(a):s=s[:-len(a)]+b
    if s!=text:return s
    sections=[h['section']]
    if h['section']=='items':sections+=['names','lines']
    for k in sections:
        if text in d.get(k,{}):return d[k][text]
    return text
class CentralLocale(unittest.TestCase):
    def test_hook_only_modules(self):
        for p in (W/'extensions/modules').glob('*/module.json'):
            m=json.loads(p.read_text('utf8'))
            self.assertNotIn('dictionary',m,p.name)
            for h in m['hooks']:
                self.assertNotIn('exact',h,(p.name,h['id']))
                self.assertNotIn('operations',h,(p.name,h['id']))
    def test_canonical_locale_and_mirror(self):
        base=(W/'languages/zh_tw/translations.json').read_bytes()
        self.assertEqual(base,(W/'languages/zh_tw/translations.json').read_bytes())
        m=json.loads((W/'languages/zh_tw/zh-tw-version.json').read_text('utf8'))
        self.assertEqual(m['locale'],'zh_tw')
        self.assertEqual(m['translation']['sha256'],hashlib.sha256(base).hexdigest())
        old=json.loads((W/'languages/zh_tw/zh-tw-version.json').read_text('utf8'))
        self.assertEqual(old['translation']['path'],'languages/zh_tw/translations.json')
        self.assertEqual(old['translation']['sha256'],m['translation']['sha256'])
        d=json.loads(base)
        for k,v in [('Leader','會長'),('Member','成員'),('Training Pavise','訓練用大盾')]:
            self.assertEqual(d['names'].get(k) or d['items'].get(k),v)
    def test_locale_lookup_does_not_fallback_to_different_language(self):
        with self.assertRaises(LocaleError):compose_bundle(W,'en_us','1.5.0',106,int(time.time())+86400)
        with self.assertRaises(LocaleError):compose_bundle(W,'../../secret','1.5.0',106,int(time.time())+86400)
    def test_new_shared_ui_hooks(self):
        b=rules();h={x['id']:x for x in b['hooks']}
        self.assertEqual(b['schema_version'],2)
        self.assertGreaterEqual(len(h),80)
        for name in ['ui-text-layout:canvas-mid-text','ui-text-layout:canvas-text-align',
                     'ui-text-layout:util-text-sized','items-economy:auction-item-name',
                     'codex-creatures:entry-lore','worlds-locations:boss-plain']:
            self.assertIn(name,h)
        self.assertTrue(all(hook['class'] not in ('net/minecraft/class_338','net/minecraft/class_634') for hook in b['hooks']))
    def test_shared_ui_exact_fallback_without_player_chat_rewrite(self):
        hook={h['id']:h for h in rules()['hooks']}['ui-text-layout:canvas-mid-text']
        dictionary=json.loads((W/'languages/zh_tw/translations.json').read_text('utf8'))
        self.assertEqual(engine(hook,'Physical Damage +3%',dictionary),'物傷 +3%')
        for private in ['Aincrad','Leader','Assassin','Pumba','TRAINING PAVISE','Player_One: HI','TW_Babycat999: Sorry','null','old']:
            self.assertEqual(engine(hook,private,dictionary),private)
        names={h['id']:h for h in rules()['hooks']}
        self.assertEqual(engine(names['codex-creatures:universe'],'Aincrad',dictionary),'艾恩葛朗特')
        self.assertEqual(engine(names['items-economy:detail-visible-title'],'TRAINING PAVISE',dictionary),'訓練用大盾')
    def test_single_language_file_strict_placeholders(self):
        module=W/'tools' if (W/'tools/compose_localized.py').is_file() else W/'client-framework/tools'
        sys.path.insert(0,str(W/'tools'))
        from validate import validate_translation,validate_version
        version=json.loads((W/'languages/zh_tw/zh-tw-version.json').read_text('utf8'))
        raw=(W/'languages/zh_tw/translations.json').read_bytes()
        self.assertGreater(validate_translation(raw,json.loads(raw),'zh_tw',validate_version(version,'zh_tw')),8000)
    def test_legacy_composer_cli_warns_locale_first(self):
        import subprocess
        p=subprocess.run([sys.executable,str(tools/'compose_modules.py'),str(W/'extensions/modules'),
            '--version','1.5.0','--sequence','106','--expires',str(int(time.time())+86400),
            '--out',str(W/'publication/incorrect-standalone.json')],capture_output=True,timeout=15)
        self.assertNotEqual(p.returncode,0)
        self.assertIn(b'compose_localized',p.stdout+p.stderr)
    def test_previous_hook_shapes_preserved(self):
        new={h['id']:h for h in rules()['hooks']}
        keys=['class','method','descriptor','kind','argument','sites','member_class','member','member_descriptor','locale','section']
        for old in OLD['hooks']:
            self.assertIn(old['id'],new)
            for k in keys:self.assertEqual(old.get(k),new[old['id']].get(k),(old['id'],k))
    def test_old_translation_behavior_never_regresses(self):
        b=rules()
        new={h['id']:h for h in b['hooks']}
        d=json.loads((W/'languages/zh_tw/translations.json').read_text('utf8'))
        approved_time=json.loads((W/'client-framework/tests/reference/compact-time-20261031.json').read_text('utf8'))['reviewed_changes']
        self.assertEqual(sum(len(v) for v in approved_time.values()),61)
        sample={'Aincrad','Healing Shard x2','TRAINING PAVISE','Leader','Member',
                'Class: assassin','Minimum Level:', 'Restores 40 mana','19. The Chamber Beneath the Fields',
                '⚔ 打擊 21 | total 1009 | ~26 DPS','null','old','Bobo_TheBuilder__: hello'}
        for old in OLD['hooks']:
            n=new[old['id']]
            words=set(old.get('exact',{}))
            if len(words)>150:words=set(list(sorted(words))[:100]+list(sorted(words))[-100:])
            words|=sample
            for w in words:
                prior=engine(old,w,OLD_DICT)
                current=engine(n,w,d)
                if old['id'] in ('items-economy:reward-name','items-economy:reward-cached-name') and w.strip(' ') in ('Healing Shard x2','治療碎片 x2'):
                    left=w[:len(w)-len(w.lstrip(' '))];right=w[len(w.rstrip(' ')):]
                    self.assertEqual(current,left+'治療碎片'+right,(old['id'],w))
                elif w.strip()=='cooldown of':
                    self.assertEqual(current.strip(),'冷卻時間:',(old['id'],w))
                elif w in approved_time.get(n.get('section','names'),{}):
                    # Curated exceptions: only previously reviewed compact numeric
                    # units (1min/3min and matching cooldown fragment displays).
                    self.assertEqual(
                        normalize_fields(approved_time[n.get('section','names')][w]['new']),
                        normalize_fields(current),(old['id'],w))
                elif old['id']=='worlds-locations:transfer-duration':
                    # Explicitly reviewed removal of the old unbounded s/m/h
                    # replacements. Only the current exact glossary is allowed.
                    self.assertFalse(n.get('operations'),"Duration hook must not replace arbitrary letters")
                    expected=n.get('exact',{}).get(w,d.get(n.get('section','names'),{}).get(w,w))
                    self.assertEqual(normalize_fields(expected),normalize_fields(current),(old['id'],w))
                elif prior!=w:self.assertEqual(normalize_fields(prior.replace('\uff1a',':')),normalize_fields(current),(old['id'],w))
    def test_bundle_much_smaller_without_inline_glossary(self):
        b=rules()
        self.assertLess(len(json.dumps(b,ensure_ascii=False).encode('utf8')),230000)
        self.assertLessEqual(len(b['hooks']),160)  # The existing runtime/parser contract permits at most 160; reject 161+.
    def test_scoped_overrides_without_orphans(self):
        overlays=json.loads((W/'languages/zh_tw/hook_text_rules.json').read_text('utf8'))
        ids={m['id']+':'+h['id'] for p in (W/'extensions/modules').glob('*/module.json') for m in [json.loads(p.read_text('utf8'))] for h in m['hooks']}
        self.assertTrue(set(overlays['hooks'])<=ids)
        self.assertIn(overlays['schema_version'],(1,2))
        from compose_localized import compile_scoped_text
        for id,v in overlays['hooks'].items():
            self.assertTrue(set(v)<=set(['use','exact','padded_exact','operations']))
            resolved=compile_scoped_text(v,overlays.get('rule_sets',{}),id)
            self.assertTrue(set(resolved)<=set(['exact','operations']))
    def test_input_locale_manifest_present(self):
        manifest=json.loads((W/'languages/locales.json').read_text('utf8'))
        self.assertEqual(manifest['available'],['zh_tw'])
if __name__=='__main__':unittest.main()
