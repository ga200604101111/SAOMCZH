package sao.zh.compat16;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Offline only: calls Minecraft 1.21.4 resource-pack repository using local installed JARs. */
public final class OfflineRepoHarness {
    public static final class MockPlayer {
        private final java.util.UUID uuid;
        public MockPlayer(java.util.UUID id){ uuid=id; }
        public java.util.UUID method_5667(){ return uuid; }
    }
    public static void main(String[] args) throws Exception {
        final ClassLoader cl = OfflineRepoHarness.class.getClassLoader();
        Class<?> versionInterface = cl.loadClass("net.minecraft.class_6489");
        final Object dataVersion = cl.loadClass("net.minecraft.class_6595").getConstructor(int.class).newInstance(4189);
        Object version = Proxy.newProxyInstance(cl, new Class<?>[] {versionInterface},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] values) {
                    String n = method.getName();
                    if ("method_48017".equals(n)) return Integer.valueOf(46);
                    if ("method_48018".equals(n)) return "1.21.4";
                    if ("method_48019".equals(n)) return "1.21.4";
                    if ("method_48020".equals(n)) return Integer.valueOf(0);
                    if ("method_48021".equals(n)) return new Date();
                    if ("method_48022".equals(n)) return Boolean.TRUE;
                    if ("method_37912".equals(n)) return dataVersion;
                    if ("toString".equals(n)) return "OfflineGameVersion";
                    if ("hashCode".equals(n)) return Integer.valueOf(1);
                    if ("equals".equals(n)) return proxy == values[0];
                    throw new UnsupportedOperationException("Not a game client: " + n);
                }
            });
        Class<?> shared = cl.loadClass("net.minecraft.class_155");
        shared.getMethod("method_34872", versionInterface).invoke(null, version);
        System.out.println("OFFLINE_VERSION_INSTALLED");
        // Vanilla bootstrap is performed by Minecraft before ResourcePackManager in real launches.
        cl.loadClass("net.minecraft.class_2966").getMethod("method_12851").invoke(null);
        System.out.println("OFFLINE_REGISTRIES_BOOTSTRAPPED");

        Class<?> provider = cl.loadClass("net.minecraft.class_3285");
        Class<?> manager = cl.loadClass("net.minecraft.class_3283");
        Object providers = Array.newInstance(provider,0);
        Object repo = manager.getConstructor(providers.getClass()).newInstance(providers);
        manager.getMethod("method_14445").invoke(repo);

        String packId = "file/SAO_TW_v9.zip";
        Object profile = manager.getMethod("method_14449",String.class).invoke(repo,packId);
        if (profile == null) throw new AssertionError("Missing external ZIP profile " + packId);
        Object support=manager.getMethod("method_14449",String.class).invoke(repo,"file/SAO_ZH_v16_support.zip");
        if(support==null)throw new AssertionError("Support pack missing");
        System.out.println("EXTERNAL_PROFILE_DISCOVERED " + packId);

        List<?> enabled = (List<?>) manager.getMethod("method_14444").invoke(repo);
        if (enabled.isEmpty() || enabled.get(enabled.size()-1) != support) {
            throw new AssertionError("External ZIP not enabled at highest priority");
        }
        System.out.println("EXTERNAL_PROFILE_ACTIVE_LAST " + enabled.size());

        manager.getMethod("method_14447",java.util.Collection.class).invoke(repo,Collections.emptyList());
        enabled = (List<?>) manager.getMethod("method_14444").invoke(repo);
        if (enabled.isEmpty() || enabled.get(enabled.size()-1) != support) {
            throw new AssertionError("Resource-pack selection reset");
        }
        System.out.println("EXTERNAL_STAYS_ACTIVE_AFTER_SELECTION");
        manager.getMethod("method_14445").invoke(repo);
        Object rescanned = manager.getMethod("method_14449",String.class).invoke(repo,packId);
        enabled = (List<?>) manager.getMethod("method_14444").invoke(repo);
        if (rescanned == null || enabled.get(enabled.size()-1) != support) {
            throw new AssertionError("External resource pack lost after rescan");
        }
        System.out.println("EXTERNAL_STAYS_ACTIVE_AFTER_RESCAN");

        Object zipPack = profile.getClass().getMethod("method_14458").invoke(profile);
        if (zipPack == null) throw new AssertionError("No zip pack factory output");
        Class<?> resourceType = cl.loadClass("net.minecraft.class_3264");
        Object clientAssets = resourceType.getField("field_14188").get(null);
        Class<?> identifier = cl.loadClass("net.minecraft.class_2960");
        Object path = identifier.getMethod("method_60655",String.class,String.class)
            .invoke(null,"saoui","lang/zh_tw.json");
        Object supplier = zipPack.getClass().getMethod("method_14405",resourceType,identifier)
            .invoke(zipPack,clientAssets,path);
        if (supplier == null) throw new AssertionError("Missing saoui/lang/zh_tw.json in external ZIP");
        Class<?> supplierInterface = cl.loadClass("net.minecraft.class_7367");
        InputStream in = (InputStream) supplierInterface.getMethod("get").invoke(supplier);
        byte[] bytes = new byte[256];
        int n = in.read(bytes);
        in.close();
        if (n <= 0) throw new AssertionError("Empty external resource");
        String text = new String(bytes,0,n,StandardCharsets.UTF_8);
        if (text.indexOf('{') < 0) throw new AssertionError("Resource not a JSON object");
        ((AutoCloseable) zipPack).close();
        System.out.println("REAL_EXTERNAL_ZIP_OPENED " + n + " prefix bytes");
        
        Class<?> bridge=Class.forName("sao.zh.compat16.RuntimeBridge",true,null);
        Class<?> table=cl.loadClass("fr.sao.translate.LangTable");
        java.lang.reflect.Constructor<?> tableCtor=table.getDeclaredConstructor(String.class,String.class,Map.class,Map.class,Map.class);tableCtor.setAccessible(true);
        Map<String,String> names=new java.util.LinkedHashMap<>();names.put("Sanglier corrompu","Corrupted Boar");
        Object nativeTable=tableCtor.newInstance("en_us","offline-original-table",names,new java.util.LinkedHashMap<String,String>(),new java.util.LinkedHashMap<String,String>());
        bridge.getMethod("awaitWriter").invoke(null);
        if(!"Corrupted Boar".equals(names.get("Sanglier corrompu")))throw new AssertionError("Original table changed");
        System.out.println("NATIVE_LANGTABLE_CAPTURED_NO_MUTATION");
        Class<?> state=cl.loadClass("fr.sao.equipment.EquipmentClient$State");
        java.lang.reflect.Constructor<?> sc=state.getDeclaredConstructors()[0];sc.setAccessible(true);
        Object target=sc.newInstance("YingHuang09","\ue07d",0L,0L,"1","Assassin",Collections.emptyList(),Collections.emptyList(),0L,1L);
        Object other=sc.newInstance("AnotherPlayer","\ue07d",0L,0L,"1","Assassin",Collections.emptyList(),Collections.emptyList(),0L,1L);
        if(!"\ue9f0".equals(state.getMethod("grade").invoke(target)))throw new AssertionError("Native target badge");
        if(!"\ue07d".equals(state.getMethod("grade").invoke(other)))throw new AssertionError("Other player altered");
        Field gf=state.getDeclaredField("grade");gf.setAccessible(true);if(!"\ue07d".equals(gf.get(target)))throw new AssertionError("Server rank backing field changed");
        System.out.println("NATIVE_BADGE_ISOLATION_PASS");
        Class<?> textType=cl.loadClass("net.minecraft.class_2561"),mutable=cl.loadClass("net.minecraft.class_5250"),styleType=cl.loadClass("net.minecraft.class_2583");
        Object sample=textType.getMethod("method_43470",String.class).invoke(null,"Corrupted Boar");
        Object style=styleType.getField("field_24360").get(null);style=styleType.getMethod("method_36139",int.class).invoke(style,0x55ccff);mutable.getMethod("method_10862",styleType).invoke(sample,style);
        Method translate=bridge.getDeclaredMethod("translateTree",Object.class,ClassLoader.class,String.class,String.class,int.class);translate.setAccessible(true);
        Object translated=translate.invoke(null,sample,cl,"names","zh_tw",0);
        if(!"腐化野豬".equals(textType.getMethod("getString").invoke(translated)))throw new AssertionError("Native Text translation");
        if(!style.equals(textType.getMethod("method_10866").invoke(translated)))throw new AssertionError("Native Text style lost");
        if(!"Corrupted Boar".equals(textType.getMethod("getString").invoke(sample)))throw new AssertionError("Native original Text mutated");
        // Original main-menu flag button: a fourth zh_tw Entry should be registered.
        Class<?> menuLanguage=cl.loadClass("fr.sao.triggersmod.screen.LanguageSelector");
        List<?> options=(List<?>)menuLanguage.getField("ENTRIES").get(null);
        if(options.size()!=4)throw new AssertionError("Expected 4 flags, got "+options.size());
        Object tw=options.get(3);
        if(!"zh_tw".equals(tw.getClass().getMethod("code").invoke(tw)))throw new AssertionError("Missing zh_tw flag");
        System.out.println("NATIVE_TAIWAN_LANGUAGE_ENTRY_PASS");
        Object resourcePath=identifier.getMethod("method_60655",String.class,String.class)
            .invoke(null,"triggersmod","textures/common/flag_tw.png");
        Object supportPack=support.getClass().getMethod("method_14458").invoke(support);
        Object flagStream=supportPack.getClass().getMethod("method_14405",resourceType,identifier)
            .invoke(supportPack,clientAssets,resourcePath);
        if(flagStream==null)throw new AssertionError("Taiwan flag resource missing");
        ((AutoCloseable)supportPack).close();
        System.out.println("NATIVE_TAIWAN_FLAG_RESOURCE_PASS");
        // A nonliteral Text parent with children must not duplicate rendered siblings.
        Object rootText=textType.getMethod("method_43471",String.class).invoke(null,"__test_node__");
        Object icon=textType.getMethod("method_43470",String.class).invoke(null,"");
        Object name=textType.getMethod("method_43470",String.class).invoke(null," YingHuang09: hello");
        mutable.getMethod("method_10852",textType).invoke(rootText,icon);
        mutable.getMethod("method_10852",textType).invoke(rootText,name);
        java.lang.String beforeText=(String)textType.getMethod("getString").invoke(rootText);
        Method glyphMethod=bridge.getDeclaredMethod("replaceGlyphTree",Object.class,ClassLoader.class,boolean[].class,int.class);
        glyphMethod.setAccessible(true);
        Object newText=glyphMethod.invoke(null,rootText,cl,new boolean[1],0);
        String afterText=(String)textType.getMethod("getString").invoke(newText);
        if(!afterText.equals(beforeText.replace("","")))
            throw new AssertionError("Text sibling duplicate or glyph failed: "+afterText+" expected="+beforeText);
        if(!beforeText.equals(textType.getMethod("getString").invoke(rootText)))
            throw new AssertionError("Original server text mutated");
        System.out.println("NATIVE_TEXT_SIBLING_PRESERVATION_PASS");
        // Real SAOUI TabEntry record includes the UUID, which must be the only rank selector.
        Class<?> tabEntry=cl.loadClass("fr.sao.saoui.tab.TabEntry");
        Class<?> tabInfo=cl.loadClass("fr.sao.saoui.tab.TabRoster$Info");
        Class<?> playerListEntry=cl.loadClass("net.minecraft.class_640");
        java.lang.reflect.Constructor<?> tabConstructor=tabEntry.getConstructor(
            playerListEntry,java.util.UUID.class,String.class,boolean.class,tabInfo);
        java.util.UUID targetId=java.util.UUID.fromString("eddeac86-5a32-4205-b22e-9a12b30db4df");
        java.util.UUID otherId=java.util.UUID.fromString("11111111-2222-3333-4444-555555555555");
        Object ownTab=tabConstructor.newInstance(null,targetId,"YingHuang09",true,null);
        Object otherTab=tabConstructor.newInstance(null,otherId,"YingHuang09",false,null);
        Method rankPrefix=bridge.getMethod("tabPrefix",String.class,Object.class);
        String playerBadge=String.valueOf((char)0xE07D),customBadge=String.valueOf((char)0xE9F0);
        if(!customBadge.equals(rankPrefix.invoke(null,playerBadge,ownTab)))throw new AssertionError("Actual TabEntry target not replaced");
        if(!playerBadge.equals(rankPrefix.invoke(null,playerBadge,otherTab)))throw new AssertionError("Actual other TabEntry changed");
        System.out.println("NATIVE_TAB_ENTRY_UUID_PASS");
        // SAOUI draws overhead player cards independently of vanilla nameplates.
        if(!FeatureTransformer.TARGETS.contains("fr/sao/playerdisplay/card/PlayerCardRenderer$CardText"))
            throw new AssertionError("Missing SaoUI custom overhead CardText hook");
        Class<?> payload=cl.loadClass("fr.sao.playerdisplay.messaging.PlayerCardPayload");
        java.lang.reflect.Constructor<?> pc=payload.getConstructor(
          java.util.UUID.class,String.class,String.class,float.class,float.class,
          int.class,boolean.class,int.class,String.class,String.class,String.class,
          boolean.class,String.class,String.class);
        Object custom=pc.newInstance(targetId,"YingHuang09","",100f,100f,1,false,0,playerBadge,"","",false,"en","TW");
        Object otherCard=pc.newInstance(otherId,"YingHuang09","",100f,100f,1,false,0,playerBadge,"","",false,"en","TW");
        if(!customBadge.equals(rankPrefix.invoke(null,playerBadge,custom)))
          throw new AssertionError("Native PlayerCard target rank");
        if(!playerBadge.equals(rankPrefix.invoke(null,playerBadge,otherCard)))
          throw new AssertionError("Native PlayerCard unrelated rank");
        if(!playerBadge.equals(payload.getMethod("prefix").invoke(custom)))
          throw new AssertionError("Native PlayerCard source mutated");
        cl.loadClass("fr.sao.playerdisplay.card.PlayerCardRenderer$CardText").getDeclaredConstructors();
        System.out.println("NATIVE_SAOUI_PLAYER_CARD_UUID_PASS");


        // Source head nameplate is the actual Minecraft 1.21.4 render-state text field.
        Class<?> labelState=cl.loadClass("net.minecraft.class_10017");
        java.lang.reflect.Field label=labelState.getField("field_53337");
        Object targetLabel=labelState.getConstructor().newInstance();
        Object otherLabel=labelState.getConstructor().newInstance();
        Object ownText=textType.getMethod("method_43470",String.class).invoke(null,playerBadge+" YingHuang09");
        Object otherText=textType.getMethod("method_43470",String.class).invoke(null,playerBadge+" YingHuang09");
        label.set(targetLabel,ownText);label.set(otherLabel,otherText);
        Method nameplate=bridge.getMethod("nameplateState",Object.class,Object.class);
        nameplate.invoke(null,new MockPlayer(targetId),targetLabel);
        nameplate.invoke(null,new MockPlayer(otherId),otherLabel);
        if(!(customBadge+" YingHuang09").equals(textType.getMethod("getString").invoke(label.get(targetLabel))))
            throw new AssertionError("Native render state missing TLZH");
        if(!(playerBadge+" YingHuang09").equals(textType.getMethod("getString").invoke(label.get(otherLabel))))
            throw new AssertionError("Other player nameplate changed");
        if(!(playerBadge+" YingHuang09").equals(textType.getMethod("getString").invoke(ownText)))
            throw new AssertionError("Original server-rendered text mutated");
        System.out.println("NATIVE_NAMEPLATE_UUID_AND_TEXT_PASS");

        // Require each render class to VERIFY/LOAD its modified bytecode.
        cl.loadClass("fr.sao.saoui.tab.TabRenderer").getDeclaredMethods();
        cl.loadClass("net.minecraft.class_1007").getDeclaredMethods();
        cl.loadClass("net.minecraft.class_338").getDeclaredMethods();
        cl.loadClass("fr.sao.itemlang.ItemLangClient").getDeclaredMethods();
        cl.loadClass("fr.sao.daily.DailyModel$Reward").getDeclaredMethods();
        // Actual SAOUI private Known record and NONE negative-cache sentinel; no server or GL.
        Class<?> itemClass=cl.loadClass("fr.sao.itemlang.ItemLangClient");
        Class<?> itemsClass=cl.loadClass("fr.sao.itemlang.ItemLangClient$Items");
        Class<?> knownClass=cl.loadClass("fr.sao.itemlang.ItemLangClient$Known");
        java.lang.reflect.Constructor<?> itemsCtor=itemsClass.getDeclaredConstructor(String.class);itemsCtor.setAccessible(true);
        java.lang.reflect.Constructor<?> knownCtor=knownClass.getDeclaredConstructor(textType,String.class);knownCtor.setAccessible(true);
        Object itemCache=itemsCtor.newInstance("en");
        Field itemKnownField=itemsClass.getDeclaredField("known");itemKnownField.setAccessible(true);
        Map itemKnown=(Map)itemKnownField.get(itemCache);
        Field itemNoneField=itemClass.getDeclaredField("NONE");itemNoneField.setAccessible(true);Object noneItem=itemNoneField.get(null);
        itemKnown.put("Untranslated item",noneItem);
        Object knownEntry=knownCtor.newInstance(sample,"Corrupted Boar");itemKnown.put("Sanglier corrompu",knownEntry);
        Field itemByField=itemClass.getDeclaredField("BY_LANG");itemByField.setAccessible(true);
        Map nativeItems=(Map)itemByField.get(null);Map savedItems=new java.util.LinkedHashMap(nativeItems);
        try{
         nativeItems.clear();nativeItems.put("en",itemCache);
         bridge.getMethod("snapshotItemlang",Class.class).invoke(null,itemClass);bridge.getMethod("awaitWriter").invoke(null);
         Field bridgeRoot=bridge.getDeclaredField("root");bridgeRoot.setAccessible(true);
         java.nio.file.Path itemRows=((java.nio.file.Path)bridgeRoot.get(null)).resolve("SAO_TW_Dynamic/review/item_seen.jsonl");
         if(!java.nio.file.Files.exists(itemRows))throw new AssertionError("Actual SAOUI item capture still fails");
         List<String> observations=java.nio.file.Files.readAllLines(itemRows,StandardCharsets.UTF_8);
         if(observations.size()!=1||!observations.get(0).contains("Sanglier corrompu"))throw new AssertionError("Unexpected native item observations");
         bridge.getMethod("snapshotItemlang",Class.class).invoke(null,itemClass);bridge.getMethod("awaitWriter").invoke(null);
         if(java.nio.file.Files.readAllLines(itemRows,StandardCharsets.UTF_8).size()!=1)throw new AssertionError("Native item capture not deduplicated");
         if(itemKnown.get("Untranslated item")!=noneItem||itemKnown.get("Sanglier corrompu")!=knownEntry)throw new AssertionError("Native cache mutated");
         Object itemTranslated=translate.invoke(null,sample,cl,"items","zh_tw",0);
         if(!"腐化野豬".equals(textType.getMethod("getString").invoke(itemTranslated)))throw new AssertionError("Item section ignored reviewed names");
         if(!style.equals(textType.getMethod("method_10866").invoke(itemTranslated)))throw new AssertionError("Item style lost");
         if(!"Corrupted Boar".equals(textType.getMethod("getString").invoke(sample)))throw new AssertionError("Original item Text mutated");
         bridge.getMethod("itemTooltip",List.class,Class.class);
         System.out.println("NATIVE_ITEM_PRIVATE_KNOWN_NONE_CAPTURE_AND_TEXT_STYLE_PASS");
        }finally{nativeItems.clear();nativeItems.putAll(savedItems);}
        System.out.println("NATIVE_DAILY_REWARD_DESCRIPTION_CLASS_PASS");
        System.out.println("NATIVE_ADDITIONAL_RENDER_AND_ITEM_CLASSES_LOAD_PASS");

        cl.loadClass("net.minecraft.class_377").getDeclaredMethods();
        cl.loadClass("com.eighteen.saojob.skilltree.NodeHoverPopup").getDeclaredMethods();
        cl.loadClass("fr.sao.translate.TranslateClient").getDeclaredMethods();
        Class<?> layout=Class.forName("sao.zh.compat16.CjkLayout",true,null);
        if(!((Boolean)layout.getMethod("containsCjk",String.class).invoke(null,"中文測試")))throw new AssertionError("Bootstrap layout helper missing");
        bridge.getMethod("wrapHoverDescription",List.class,String.class,Object.class,int.class,int.class,Class.class);
        System.out.println("NATIVE_CJK_LAYOUT_BRIDGE_AND_POPUP_VERIFIED");
        Class<?> splashClass=cl.loadClass("fr.sao.saoui.splash.SplashOverlayRenderer");
        splashClass.getDeclaredMethods();
        Class<?> splashBridge=Class.forName("sao.zh.compat16.SplashBridge",true,null);
        splashBridge.getMethod("verifyNativeContracts",Class.class).invoke(null,splashClass);
        java.lang.reflect.Method readSplash=splashClass.getDeclaredMethod("readSplashLang",String.class);
        readSplash.setAccessible(true);
        Map<?,?> zhSplash=(Map<?,?>)readSplash.invoke(null,"zh_tw");
        Map<?,?> enSplash=(Map<?,?>)readSplash.invoke(null,"en_us");
        if(!"載入中".equals(zhSplash.get("saoui.splash.loading")))throw new AssertionError("Native startup label missing");
        if(!"LOADING".equals(enSplash.get("saoui.splash.loading")))throw new AssertionError("English startup changed");
        for(int tip=1;tip<=52;tip++)if(zhSplash.get("saoui.splash.tip."+tip)==null)throw new AssertionError("Native startup tip missing "+tip);
        java.lang.reflect.Constructor<?> splashCtor=splashClass.getDeclaredConstructor();splashCtor.setAccessible(true);Object splash=splashCtor.newInstance();
        java.lang.reflect.Method splashWrap=splashClass.getDeclaredMethod("wrapPhraseRaw",String.class,int.class,int.class);splashWrap.setAccessible(true);
        List<?> wrapped=(List<?>)splashWrap.invoke(splash,zhSplash.get("saoui.splash.tip.7"),18,200);
        if(wrapped.size()<2)throw new AssertionError("Native startup CJK does not wrap");
        System.out.println("NATIVE_STARTUP_ZH_53_TEXTS_AND_TEXTURE_API_CONTRACTS_PASS no_GL_no_game_started");
        fixture.NativeHotProbe.verify();
        System.out.println("NATIVE_TEXT_STYLE_AND_CLASS_VERIFICATION_PASS");
        System.out.println("OFFLINE_REPOSITORY_INTEGRATION_PASS");
    }
}
