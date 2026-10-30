package sao.zh.engine;
import java.util.*;import java.util.function.*;
/** Reviewed paragraph sequences only: no inference from sentence punctuation or line length. */
public final class LoreParagraphContract {
 static int checks;static final String EN="A material of nature,\nused mainly\nfor light and fast\narmor.";
 static final String FR="Matériau de la nature,\nutilisé principalement\npour des armures légères\net rapides.";
 static final String ZH="一種天然材料，主要用於製作輕巧靈活的護甲。";
 static final Map<String,Map<String,String>> DATA=Map.of("items",Map.ofEntries(
  Map.entry(EN,ZH),Map.entry(FR,ZH),Map.entry("A material of nature,","一種天然材料，"),Map.entry("used mainly","主要用於"),Map.entry("for light and fast","適合輕巧而迅速的"),Map.entry("armor.","護甲。")),"lines",Map.of(),"names",Map.of());
 static void eq(Object a,Object b,String what){checks++;if(!Objects.equals(a,b))throw new AssertionError(what+" expected="+b+" actual="+a);}
 static Object call(LoreParagraphs plan,Object input){return plan.project(input,UnaryOperator.identity(),UnaryOperator.identity(),(a,b)->b,1L);}
 public static void main(String[]args){
  Script.Hook h=new Script.Hook(Map.of("id","items-economy:tooltip-description-paragraphs","class","fr/sao/tooltip/TipModel","method","description","descriptor","()Ljava/util/List;","kind","description_list","section","items","capture","item"));
  eq(h.kind,"description_list","hook accepted");
  for(String bad:List.of("effects","actions","stats")){
   try{new Script.Hook(Map.of("id",h.id,"class",h.owner,"method",bad,"descriptor",h.descriptor,"kind",h.kind,"section",h.section));throw new AssertionError("broad hook accepted "+bad);}catch(IllegalArgumentException correct){checks++;}
  }
  LoreParagraphs plan=new LoreParagraphs(DATA);
  List<String> en=List.of(EN.split("\n"));eq(call(plan,en),List.of(ZH),"reviewed english paragraph");eq(en.size(),4,"original source count");eq(en.get(0),"A material of nature,","original source text");
  eq(call(plan,List.of(FR.split("\n"))),List.of(ZH),"reviewed french paragraph");
  List<String> localized=List.of("一種天然材料，","主要用於","適合輕巧而迅速的","護甲。");
  eq(call(plan,localized),List.of(ZH),"already-translated fragments rejoin through derived dictionary index");
  List<String> colored=en.stream().map(s->"§7 "+s).toList();eq(call(plan,colored),List.of("§7 "+ZH),"uniform color and indentation");
  List<String> mixed=List.of("§7 A material of nature,","§6 used mainly","§7 for light and fast","§7 armor.");eq(call(plan,mixed),mixed,"mixed styles not joined");
  List<String> blank=List.of("A material of nature,","","used mainly","for light and fast","armor.");eq(call(plan,blank),blank,"paragraph separator retained");
  List<String> intentional=List.of("A material of nature,","used mainly","Unknown deliberate text","armor.");eq(call(plan,intentional),intentional,"unknown sequence retains all hard breaks");
  List<String> structured=List.of("Attack: +3","Defense: +4","Lv. 1+","You have: 0/12","→ Right-click","§7 ");eq(call(plan,structured),structured,"fields and empty format rows preserved");
  List<String> multi=List.of("A material of nature,\nused mainly","for light and fast","armor.");eq(call(plan,multi),multi,"explicit in-row newline preserved");
  List<String> icons=List.of("\uea72A material of nature,","used mainly","for light and fast","armor.");eq(call(plan,icons),icons,"font icon boundary retained");
  List<String> duplicate=new ArrayList<>(en);duplicate.add("");duplicate.addAll(en);eq(call(plan,duplicate),List.of(ZH,"",ZH),"two paragraphs separated");
  eq(call(plan,null),null,"null fail open");eq(call(plan,List.of(12)),List.of(12),"wrong list type fail open");
  List<String> many=Collections.nCopies(129,"armor.");eq(call(plan,many)==many,true,"list budget identity");
  List<String> huge=List.of("x".repeat(8193));eq(call(plan,huge)==huge,true,"text budget identity");
  Map<String,Map<String,String>> updated=Map.of("items",Map.of(EN,"另一個已審核的段落。"));
  eq(call(new LoreParagraphs(updated),en),List.of("另一個已審核的段落。"),"new dictionary does not use stale paragraph");
  Object one=call(plan,en),two=call(plan,en);eq(one==two,true,"repeat complete paragraph uses bounded memo");
  Object changed=plan.project(en,UnaryOperator.identity(),UnaryOperator.identity(),(a,b)->"[new glyph mapping]"+b,2L);eq(changed,List.of("[new glyph mapping]"+ZH),"epoch invalidates encoded memo");
  eq(call(plan,List.of("Lv. 1+")),List.of("Lv. 1+"),"native level unchanged");
  System.out.println("LORE_PARAGRAPH_CONTRACT_PASS checks="+checks+" original_immutable=true unknown_linebreaks_preserved=true");
 }
}
