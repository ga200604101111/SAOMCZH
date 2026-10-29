package sao.zh.compat16;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;

/** Precise return-value instrumentation; no network, authorization or server state changes. */
public final class FeatureTransformer implements ClassFileTransformer {
 static final String BRIDGE="sao/zh/compat16/RuntimeBridge";
 static final String TABLE="fr/sao/translate/LangTable",TRANSLATE="fr/sao/translate/TranslateClient",STATE="fr/sao/equipment/EquipmentClient$State",FONT="net/minecraft/class_377",HOVER="com/eighteen/saojob/skilltree/NodeHoverPopup",TAB="fr/sao/saoui/tab/TabRenderer",HEAD="net/minecraft/class_1007",CHAT="net/minecraft/class_338",LANG="fr/sao/triggersmod/screen/LanguageSelector",ITEM="fr/sao/itemlang/ItemLangClient",CARD="fr/sao/playerdisplay/card/PlayerCardRenderer$CardText",DAILY_REWARD="fr/sao/daily/DailyModel$Reward",
 EQUIP_STAT="fr/sao/equipment/EquipmentClient$Stat",SKILL_DETAIL="com/eighteen/saojob/skills/SkillDetailPopup",
 BOSS="net/minecraft/class_337",HUD="net/minecraft/class_329",
 MOB_SPLIT="fr/sao/saoui/mob/MobNameSplit",ACTION_TRANSLATOR="fr/sao/saoui/hud/actionbar/ActionBarTranslator";
 static final Set<String>TARGETS=new HashSet<>(Arrays.asList(TABLE,TRANSLATE,STATE,FONT,HOVER,TAB,HEAD,CHAT,LANG,ITEM,CARD,DAILY_REWARD,EQUIP_STAT,SKILL_DETAIL,BOSS,HUD,MOB_SPLIT,ACTION_TRANSLATOR));
 public byte[] transform(ClassLoader loader,String name,Class<?> redef,ProtectionDomain domain,byte[] bytes){
  if(!TARGETS.contains(name)||bytes==null)return null;
  try{byte[] next=patch(name,bytes);DirectAgent.log("FEATURE_TRANSFORMED "+name);return next;}
  catch(Throwable e){DirectAgent.log("FEATURE_SKIPPED "+name+" "+e.getClass().getSimpleName());return null;}
 }
 static byte[] patch(final String target,byte[] input){
  ClassReader reader=new ClassReader(input);if(!target.equals(reader.getClassName()))throw new IllegalArgumentException("Class mismatch");
  ClassWriter out=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);final int[]hooks={0},layoutHooks={0};final Set<String>found=new HashSet<>();
  reader.accept(new ClassVisitor(Opcodes.ASM9,out){
   public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[]exceptions){
    MethodVisitor parent=super.visitMethod(access,name,desc,sig,exceptions);
    boolean ctor=target.equals(TABLE)&&name.equals("<init>")&&desc.equals("(Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;)V");
    boolean delta=target.equals(TABLE)&&name.equals("apply")&&desc.equals("(Lcom/google/gson/JsonObject;Lcom/google/gson/JsonObject;Ljava/lang/String;)I");
    boolean grade=target.equals(STATE)&&name.equals("grade")&&desc.equals("()Ljava/lang/String;");
    boolean baked=target.equals(FONT)&&name.equals("method_41832")&&desc.equals("(I)Lnet/minecraft/class_382;");
    boolean metric=target.equals(FONT)&&name.equals("method_2011")&&desc.equals("(IZ)Lnet/minecraft/class_379;");
    boolean string=target.equals(TRANSLATE)&&desc.equals("(Ljava/lang/String;)Ljava/lang/String;")&&Arrays.asList("names","wholeName","title","messageLegacy").contains(name);
    boolean text=target.equals(TRANSLATE)&&desc.endsWith(")Lnet/minecraft/class_2561;")&&Arrays.asList("names","lines","linesLoose","titleText","dialogue","display","messages","entityName").contains(name);
    boolean hover=target.equals(HOVER)&&name.equals("render")&&desc.equals("(Lnet/minecraft/class_332;Lnet/minecraft/class_437;Lnet/minecraft/class_327;Lcom/eighteen/saojob/skilltree/MapNode;IIZ)V");
    boolean tab=target.equals(TAB)&&name.equals("rowText")&&desc.equals("(Lnet/minecraft/class_332;Lfr/sao/shape/DesignSpace;Lnet/minecraft/class_327;FFLfr/sao/saoui/tab/TabEntry;F)V");
    boolean head=target.equals(HEAD)&&name.equals("method_62604")&&desc.equals("(Lnet/minecraft/class_742;Lnet/minecraft/class_10055;F)V");
    boolean chat=target.equals(CHAT)&&((name.equals("method_1812")&&desc.equals("(Lnet/minecraft/class_2561;)V"))||(name.equals("method_44811")&&desc.equals("(Lnet/minecraft/class_2561;Lnet/minecraft/class_7469;Lnet/minecraft/class_7591;)V")));
    boolean lang=target.equals(LANG)&&name.equals("<clinit>")&&desc.equals("()V");
    boolean itemhandle=target.equals(ITEM)&&name.equals("handle")&&desc.equals("(Ljava/lang/String;Lcom/google/gson/JsonObject;)V");
    boolean itemstring=target.equals(ITEM)&&name.equals("translate")&&desc.equals("(Ljava/lang/String;)Ljava/lang/String;");
    boolean itemtext=target.equals(ITEM)&&name.equals("name")&&desc.equals("(Lnet/minecraft/class_1799;Lnet/minecraft/class_2561;)Lnet/minecraft/class_2561;");
    boolean itemname=target.equals(ITEM)&&(name.equals("name")||name.equals("knownPlain"))&&desc.equals("(Ljava/lang/String;)Ljava/lang/String;");
    boolean itemlore=target.equals(ITEM)&&name.equals("lore")&&desc.equals("(Lnet/minecraft/class_1799;Ljava/util/List;)V");
    boolean dailyDesc=target.equals(DAILY_REWARD)&&name.equals("desc")&&desc.equals("()Ljava/lang/String;");
    boolean equipStat=target.equals(EQUIP_STAT)&&name.equals("name")&&desc.equals("()Ljava/lang/String;");
    boolean skillDetail=target.equals(SKILL_DETAIL);
    boolean boss=target.equals(BOSS)&&name.equals("method_1796")&&desc.equals("(Lnet/minecraft/class_332;)V");
    boolean action=target.equals(HUD)&&name.equals("method_1758")&&desc.equals("(Lnet/minecraft/class_2561;Z)V");
    boolean scoreboard=target.equals(HUD)&&name.equals("method_1757")&&desc.equals("(Lnet/minecraft/class_332;Lnet/minecraft/class_266;)V");
    boolean mob=target.equals(MOB_SPLIT)&&name.equals("split")&&desc.equals("(Ljava/lang/String;)[Ljava/lang/String;");
    boolean actionTranslate=target.equals(ACTION_TRANSLATOR)&&name.equals("translate")&&desc.equals("(Lnet/minecraft/class_2561;)Lnet/minecraft/class_2561;");
    boolean card=target.equals(CARD)&&name.equals("<init>")&&desc.equals("(Lfr/sao/playerdisplay/messaging/PlayerCardPayload;Lnet/minecraft/class_638;)V");
    if(!(ctor||delta||grade||baked||metric||string||text||hover||tab||head||chat||lang||itemhandle||itemstring||itemtext||itemname||itemlore||card||dailyDesc||equipStat||skillDetail||boss||action||scoreboard||mob||actionTranslate))return parent;
    found.add(name);String section=name.equals("messages")||name.equals("messageLegacy")?"messages":(name.equals("lines")||name.equals("linesLoose")||name.equals("dialogue")||name.equals("display")?"lines":"names");
    return new MethodVisitor(Opcodes.ASM9,parent){
     boolean missingBranch=false;
     public void visitCode(){
      super.visitCode();
      if(action){
       super.visitVarInsn(Opcodes.ALOAD,1);
       super.visitLdcInsn(Type.getObjectType(HUD));super.visitLdcInsn("lines");super.visitLdcInsn("actionbar_system");
       call("localizeUiText","(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
       super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");super.visitVarInsn(Opcodes.ASTORE,1);
      }
      if(chat){
       super.visitVarInsn(Opcodes.ALOAD,1);
       call("chatText","(Ljava/lang/Object;)Ljava/lang/Object;");
       super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");
       super.visitVarInsn(Opcodes.ASTORE,1);
       if(name.equals("method_44811")){
        super.visitVarInsn(Opcodes.ALOAD,1);super.visitVarInsn(Opcodes.ALOAD,2);super.visitVarInsn(Opcodes.ALOAD,3);
        call("systemChat","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");super.visitVarInsn(Opcodes.ASTORE,1);
       }
      }
     }
     public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean isInterface){
      boolean wrap=hover&&opcode==Opcodes.INVOKESTATIC&&owner.equals(HOVER)&&method.equals("splitDescription")&&descriptor.equals("(Ljava/lang/String;)Ljava/util/List;");
      if(wrap)super.visitInsn(Opcodes.DUP); // Keep full translated source, including blank paragraphs.
      super.visitMethodInsn(opcode,owner,method,descriptor,isInterface);
      if(boss&&opcode==Opcodes.INVOKEVIRTUAL&&owner.equals("net/minecraft/class_345")
          &&method.equals("method_5414")&&descriptor.equals("()Lnet/minecraft/class_2561;")){
       super.visitLdcInsn(Type.getObjectType(BOSS));super.visitLdcInsn("names");super.visitLdcInsn("bossbar_title");
       call("localizeUiText","(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
       super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");
      }
      if(scoreboard&&opcode==Opcodes.INVOKEVIRTUAL&&owner.equals("net/minecraft/class_266")
          &&method.equals("method_1114")&&descriptor.equals("()Lnet/minecraft/class_2561;")){
       super.visitLdcInsn(Type.getObjectType(HUD));super.visitLdcInsn("names");super.visitLdcInsn("scoreboard_title");
       call("localizeUiText","(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
       super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");
      }
      if(wrap){
       super.visitInsn(Opcodes.SWAP); // original list, translated source
       super.visitVarInsn(Opcodes.ALOAD,2); // native TextRenderer
       super.visitVarInsn(Opcodes.ILOAD,13); // popup width
       super.visitInsn(Opcodes.ICONST_2);
       super.visitVarInsn(Opcodes.ILOAD,14); // horizontal padding
       super.visitInsn(Opcodes.IMUL);super.visitInsn(Opcodes.ISUB);
       super.visitVarInsn(Opcodes.ILOAD,17); // description line height, same units as the native draw
       super.visitLdcInsn(Type.getObjectType(HOVER));
       call("wrapHoverDescription","(Ljava/util/List;Ljava/lang/String;Ljava/lang/Object;IILjava/lang/Class;)Ljava/util/List;");
       layoutHooks[0]++;
      }
      if(tab&&opcode==Opcodes.INVOKEVIRTUAL
          &&owner.equals("fr/sao/saoui/tab/TabRoster$Info")
          &&method.equals("prefix")&&descriptor.equals("()Ljava/lang/String;")){
       super.visitVarInsn(Opcodes.ALOAD,5);
       call("tabPrefix","(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");
      }
      if(card&&opcode==Opcodes.INVOKEVIRTUAL&&
          owner.equals("fr/sao/playerdisplay/messaging/PlayerCardPayload")&&
          method.equals("prefix")&&descriptor.equals("()Ljava/lang/String;")){
       super.visitVarInsn(Opcodes.ALOAD,1);
       call("tabPrefix","(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");
      }
      if(head&&opcode==Opcodes.INVOKESPECIAL
          &&owner.equals("net/minecraft/class_922")&&method.equals("method_62355")
          &&descriptor.equals("(Lnet/minecraft/class_1309;Lnet/minecraft/class_10042;F)V")){
       // At the base state-initialization call site, player and state locals are still valid.
       super.visitVarInsn(Opcodes.ALOAD,1);
       super.visitVarInsn(Opcodes.ALOAD,2);
       call("nameplateState","(Ljava/lang/Object;Ljava/lang/Object;)V");
      }
     }
     public void visitFieldInsn(int opcode,String owner,String field,String descriptor){
      super.visitFieldInsn(opcode,owner,field,descriptor);
      if(baked&&opcode==Opcodes.GETFIELD&&owner.equals(FONT)&&field.equals("field_2256"))missingBranch=true;
      if(scoreboard&&opcode==Opcodes.GETFIELD&&owner.equals("net/minecraft/class_329$class_9016")
        &&field.equals("comp_2131")&&descriptor.equals("Lnet/minecraft/class_2561;")){
       super.visitLdcInsn(Type.getObjectType(HUD));super.visitLdcInsn("lines");super.visitLdcInsn("scoreboard_entry");
       call("localizeUiText","(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
       super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");
      }
      if(skillDetail&&opcode==Opcodes.GETFIELD&&owner.equals("com/eighteen/saojob/skills/SkillsData$SkillEntry")
        &&descriptor.equals("Ljava/lang/String;")&&Arrays.asList("name","description","category","blockedByName").contains(field)){
       super.visitLdcInsn(Type.getObjectType(SKILL_DETAIL));
       super.visitLdcInsn(field.equals("description")?"lines":"names");
       super.visitLdcInsn(field.equals("description")?"skill_desc":"skill_name");
       call("localizeUiString","(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
      }
      if(skillDetail&&opcode==Opcodes.GETFIELD&&owner.equals("com/eighteen/saojob/skills/SkillsData$SkillStat")
        &&field.equals("name")&&descriptor.equals("Ljava/lang/String;")){
       super.visitLdcInsn(Type.getObjectType(SKILL_DETAIL));super.visitLdcInsn("messages");super.visitLdcInsn("skill_stat");
       call("localizeUiString","(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
      }
      if(skillDetail&&opcode==Opcodes.GETFIELD&&owner.equals("com/eighteen/saojob/skills/SkillsData$SkillEntry")
        &&field.equals("descriptionLore")&&descriptor.equals("Ljava/util/List;")){
       super.visitLdcInsn(Type.getObjectType(SKILL_DETAIL));
       call("localizeLore","(Ljava/util/List;Ljava/lang/Class;)Ljava/util/List;");
      }
      if(hover&&opcode==Opcodes.GETFIELD&&owner.equals("com/eighteen/saojob/skilltree/MapNode")&&descriptor.equals("Ljava/lang/String;")&&Arrays.asList("displayName","description","category").contains(field)){
       super.visitLdcInsn(field);call("observeSkilltree","(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");super.visitLdcInsn(Type.getObjectType(HOVER));super.visitLdcInsn(field.equals("description")?"lines":"names");call("translateString","(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;");
      }
     }
     void call(String method,String descriptor){super.visitMethodInsn(Opcodes.INVOKESTATIC,BRIDGE,method,descriptor,false);hooks[0]++;}
     public void visitInsn(int opcode){
      if(opcode==Opcodes.RETURN&&itemlore){
       super.visitVarInsn(Opcodes.ALOAD,1);super.visitLdcInsn(Type.getObjectType(ITEM));
       call("itemTooltip","(Ljava/util/List;Ljava/lang/Class;)V");
      }
      if(opcode==Opcodes.RETURN&&itemhandle){
       super.visitLdcInsn(Type.getObjectType(ITEM));
       call("snapshotItemlang","(Ljava/lang/Class;)V");
      }
      if(opcode==Opcodes.RETURN&&lang){
       super.visitFieldInsn(Opcodes.GETSTATIC,LANG,"ENTRIES","Ljava/util/List;");
       super.visitLdcInsn(Type.getObjectType(LANG));
       call("extendLanguages","(Ljava/util/List;Ljava/lang/Class;)Ljava/util/List;");
       super.visitFieldInsn(Opcodes.PUTSTATIC,LANG,"ENTRIES","Ljava/util/List;");
      }
      if((ctor&&opcode==Opcodes.RETURN)||(delta&&opcode==Opcodes.IRETURN)){
       super.visitVarInsn(Opcodes.ALOAD,0);super.visitLdcInsn(ctor?"assembled":"delta_applied");call("snapshot","(Ljava/lang/Object;Ljava/lang/String;)V");
      }
      if(opcode==Opcodes.ARETURN){
       if(equipStat){
        super.visitLdcInsn(Type.getObjectType(EQUIP_STAT));super.visitLdcInsn("names");super.visitLdcInsn("equipment_stat");
        call("localizeUiString","(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
       }
       if(mob){
        super.visitLdcInsn(Type.getObjectType(MOB_SPLIT));
        call("localizeMobParts","([Ljava/lang/String;Ljava/lang/Class;)[Ljava/lang/String;");
       }
       if(actionTranslate){
        super.visitLdcInsn(Type.getObjectType(ACTION_TRANSLATOR));super.visitLdcInsn("lines");super.visitLdcInsn("actionbar_system");
        call("localizeUiText","(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
        super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");
       }
       if(itemstring||itemtext||itemname){
         super.visitLdcInsn(Type.getObjectType(ITEM));
         call((itemstring||itemname)?"itemString":"itemText",
              (itemstring||itemname)?"(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/String;":
                         "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
         if(itemtext)super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");
        }
        if(dailyDesc){super.visitLdcInsn(Type.getObjectType(DAILY_REWARD));call("dailyRewardDescription","(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/String;");}
       if(grade){super.visitVarInsn(Opcodes.ALOAD,0);call("grade","(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");}
       if(string||text){super.visitLdcInsn(Type.getObjectType(TRANSLATE));super.visitLdcInsn(section);call(string?"translateString":"translateText",string?"(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;":"(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;");if(text)super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_2561");}
       if(baked&&missingBranch){super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ILOAD,1);call("missingGlyph","(Ljava/lang/Object;Ljava/lang/Object;I)Ljava/lang/Object;");super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_382");}
       if(metric){super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ILOAD,1);super.visitVarInsn(Opcodes.ILOAD,2);call("missingMetrics","(Ljava/lang/Object;Ljava/lang/Object;IZ)Ljava/lang/Object;");super.visitTypeInsn(Opcodes.CHECKCAST,"net/minecraft/class_379");}
      }
      super.visitInsn(opcode);
     }
    };
   }
  },0);
  if(hooks[0]==0)throw new IllegalStateException("No expected hooks in "+target);
  if(target.equals(HOVER)&&layoutHooks[0]!=1)throw new IllegalStateException("Hover layout call site changed; refusing unsafe patch");
  if(target.equals(TABLE)&&!(found.contains("<init>")&&found.contains("apply")))throw new IllegalStateException("Table signature changed");
  if(target.equals(FONT)&&!(found.contains("method_41832")&&found.contains("method_2011")))throw new IllegalStateException("Font signature changed");
  if(target.equals(TAB)&&!found.contains("rowText"))throw new IllegalStateException("TabRenderer signature changed");
  if(target.equals(HEAD)&&!found.contains("method_62604"))throw new IllegalStateException("Player renderer signature changed");
  if(target.equals(CHAT)&&!found.contains("method_1812"))throw new IllegalStateException("ChatHud signature changed");
  if(target.equals(LANG)&&!found.contains("<clinit>"))throw new IllegalStateException("LanguageSelector signature changed");
  if(target.equals(CHAT)&&!(found.contains("method_1812")&&found.contains("method_44811")))throw new IllegalStateException("Unsigned or signed display chat hook missing");
  if(target.equals(ITEM)&&!(found.contains("handle")&&found.contains("translate")&&found.contains("name")&&found.contains("knownPlain")&&found.contains("lore")))throw new IllegalStateException("ItemLangClient signature changed");
  if(target.equals(DAILY_REWARD)&&!found.contains("desc"))throw new IllegalStateException("Daily Reward description accessor changed");
  if(target.equals(CARD)&&!found.contains("<init>"))throw new IllegalStateException("PlayerCard text constructor changed");
  if(target.equals(EQUIP_STAT)&&!found.contains("name"))throw new IllegalStateException("Equipment stat accessor missing");
  if(target.equals(SKILL_DETAIL)&&!found.contains("renderDescZone"))throw new IllegalStateException("Skill detail rendering surface missing");
  if(target.equals(BOSS)&&!found.contains("method_1796"))throw new IllegalStateException("Bossbar render surface missing");
  if(target.equals(HUD)&&!(found.contains("method_1758")&&found.contains("method_1757")))throw new IllegalStateException("Actionbar or scoreboard rendering surface missing");
  if(target.equals(MOB_SPLIT)&&!found.contains("split"))throw new IllegalStateException("Mob name split surface missing");
  if(target.equals(ACTION_TRANSLATOR)&&!found.contains("translate"))throw new IllegalStateException("Custom Actionbar translation surface missing");
  return out.toByteArray();
 }
 public static void main(String[]args)throws Exception{
  int total=0;for(String arg:args){try(java.util.zip.ZipFile z=new java.util.zip.ZipFile(arg)){for(String name:TARGETS){java.util.zip.ZipEntry e=z.getEntry(name+".class");if(e==null)continue;byte[]b;try(java.io.InputStream in=z.getInputStream(e)){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[]buf=new byte[8192];int n;while((n=in.read(buf))>0)bytes.write(buf,0,n);b=bytes.toByteArray();}byte[]patched=patch(name,b);new ClassReader(patched);System.out.println("BYTECODE_PASS "+name+" original="+b.length+" patched="+patched.length);total++;}}}if(total!=18)throw new AssertionError("Expected eighteen feature classes, found "+total);System.out.println("ALL_FEATURE_TARGETS_VERIFIED");
 }
}
