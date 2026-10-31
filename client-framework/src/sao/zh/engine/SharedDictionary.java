package sao.zh.engine;
import java.util.*;
/** Reuse exact reviewed terms across public content domains, not generic chat/player rendering. */
final class SharedDictionary {
 private SharedDictionary(){}
 static boolean reusable(Script.Hook h){
  if(h.section.equals("messages")||SemanticText.system(h))return false;
  return h.section.equals("items")||SemanticText.mob(h)||SemanticText.boss(h)||SemanticText.npc(h)||SemanticText.world(h)||SemanticText.compass(h)||SemanticText.location(h)
   ||h.owner.startsWith("fr/sao/codex/")||h.owner.startsWith("fr/sao/questtracker/")
   ||h.owner.startsWith("fr/sao/iteminfo/")||h.owner.startsWith("fr/sao/tooltip/")||h.owner.equals("fr/zapdialogue/client/text/StyledText")
   ||h.owner.startsWith("com/eighteen/saojob/skills/")||h.owner.startsWith("com/eighteen/saojob/skilltree/");
 }
 static String lookup(Script.Hook h,String text,Map<String,Map<String,String>> data){
  if(text==null||text.isEmpty()||text.length()>8192)return null;
  String primary=data.getOrDefault(h.section,Map.of()).get(text);
  if(primary!=null)return primary.length()<=16384?primary:null;
  if(!reusable(h))return null;
  String unique=null;
  for(String section:List.of("names","items","lines")){
   if(section.equals(h.section))continue;String value=data.getOrDefault(section,Map.of()).get(text);
   if(value==null)continue;if(value.length()>16384)return null;
   if(unique!=null&&!unique.equals(value))return null;
   unique=value;
  }
  return unique;
 }
}
