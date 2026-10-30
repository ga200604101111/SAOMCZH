package sao.zh.engine;
import java.util.*;
public final class SharedDictionaryContract {
 static int n,f;
 static Script.Hook h(String id,String owner,String section){return new Script.Hook(Map.of("id",id,"class",owner,"method","display","descriptor","(Ljava/lang/String;)Ljava/lang/String;","kind","return","section",section));}
 static void eq(String expected,String got){n++;if(!Objects.equals(expected,got)){f++;System.out.println("FAIL shared expected="+expected+" got="+got);}}
 public static void main(String[]args){
  Map<String,Map<String,String>> d=Map.of("items",Map.of("Copper Ingot","銅錠","Mini Treant","迷你樹人","Marécage Putride","腐敗沼澤","Ambiguous","物品譯法"),"lines",Map.of("Ambiguous","敘述譯法"),"names",Map.of(),"messages",Map.of());
  Script.Hook codex=h("codex-creatures:entry-name","fr/sao/codex/CodexScreen","names"),mob=h("codex-creatures:mob-shown-name","fr/sao/saoui/mob/MobBarRenderer","names"),compass=h("quests-objectives:compass-visible-title","fr/sao/questtracker/hud/QuestCompassRenderer","names"),general=h("ui-text-layout:canvas-mid-text","fr/sao/pass/screen/PassCanvas","messages"),system=h("dynamic-translation:game-system-display","net/minecraft/class_7594","messages");
  eq("銅錠",RenderTextPipeline.resolve(codex,"Copper Ingot",d));
  eq("迷你樹人 Lv. 3",RenderTextPipeline.resolve(mob,"Mini Treant Lv. 3",d));
  eq("腐敗沼澤",RenderTextPipeline.resolve(compass,"Marécage Putride",d));
  eq("Ambiguous",RenderTextPipeline.resolve(codex,"Ambiguous",d));
  Map<String,Map<String,String>> local=new HashMap<>(d);local.put("names",Map.of("Ambiguous","名稱專用"));
  eq("名稱專用",RenderTextPipeline.resolve(codex,"Ambiguous",local));
  for(Script.Hook scope:List.of(general,system))for(String text:List.of("Copper Ingot","Mini Treant","Marécage Putride","Ambiguous"))eq(text,RenderTextPipeline.resolve(scope,text,d));
  eq("Copper Ingot Extra",RenderTextPipeline.resolve(codex,"Copper Ingot Extra",d));
  if(f!=0)throw new AssertionError("Shared lookup failures="+f+"/"+n);
  System.out.println("SHARED_DICTIONARY_CONTRACT_PASS checks="+n+" exact=true conflict_fail_open=true chat_isolated=true");
 }
}
