package sao.zh.engine;
import java.util.*;import java.util.function.*;import java.lang.reflect.*;
/** Typographic regressions; widths are supplied just as they are by the real renderer. */
public final class TypographyContract {
 static int checks;
 static void ok(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static double width(String s){return s.codePoints().mapToDouble(c->c<128?.5:1).sum();}
 static List<String> wrap(String s,double width){return CjkWrap.wrap(s,width,12,TypographyContract::width);}
 static void conserved(String s,List<String> lines,double w){ok(String.join("",lines).replace(" ","").equals(s.replace(" ","")),"content changed: "+lines);for(String row:lines)ok(width(row)<=w,"overflow: "+row);}
 public static void main(String[]args)throws Exception{
  if(args.length==0||args[0].equals("wrap")){
   String shortTail="甲乙丙丁戊己庚辛壬癸子";List<String>x=wrap(shortTail,10);conserved(shortTail,x,10);ok(x.size()==2,"changed line count");ok(x.get(1).codePointCount(0,x.get(1).length())>=3,"one-character orphan remains: "+x);
   String sentence="這是一種天然的材料，主要用於製作護甲。";x=wrap(sentence,12);conserved(sentence,x,12);ok(x.get(0).endsWith("，"),"missed suitable punctuation break: "+x);
   String mixed="目前攻擊速度提升至 1.2/s";x=wrap(mixed,11);conserved(mixed,x,11);ok(x.stream().anyMatch(l->l.contains("1.2/s")),"split number/unit: "+x);
   for(String token:List.of("+15%","0/12","108°C","Lv. 12+")){String text="甲乙丙丁戊己庚 "+token;x=wrap(text,8);conserved(text,x,8);ok(x.stream().anyMatch(l->l.contains(token)),"split protected token "+token+": "+x);}
   ok(wrap("第一行\n\n第二行",8).equals(List.of("第一行","","第二行")),"intentional breaks changed");
   String p="材料可製作（輕型護甲），也可用於強化。";x=wrap(p,8);conserved(p,x,8);for(String row:x){ok(!row.startsWith("，")&&!row.startsWith("。）")&&!row.startsWith("）"),"closing punctuation at start");ok(!row.endsWith("（"),"opening punctuation at end");}
   System.out.println("TYPOGRAPHY_WRAP_PASS checks="+checks);
  }else{
   Script.Hook h=new Script.Hook(Map.of("id","items-economy:tooltip-display","class","fr/sao/tooltip/ItemTip","method","text","descriptor","(Ljava/lang/String;)Ljava/lang/String;","kind","return","section","items"));
   Map<String,Map<String,String>> d=Map.of("items",Map.of("Attack Damage : 7","攻擊傷害 : 7"));
   ok(RenderTextPipeline.resolve(h,"Attack Damage : 7",d).equals("攻擊傷害: 7"),"translated field not normalized");
   for(String source:List.of("攻擊傷害：7","攻擊傷害 : 7","攻擊傷害:7","攻擊傷害:   7"))ok(RenderTextPipeline.resolve(h,source,d).equals("攻擊傷害: 7"),"raw localized field not normalized: "+source);
   ok(RenderTextPipeline.resolve(h,"§7攻擊傷害 §f:§a7",d).equals("§7攻擊傷害§f:§a 7"),"styles or spacing corrupted");
   for(String value:List.of("Lv. 1+","12:30","https://example.com/a:b","minecraft:stone","比例 1:2","Player_One: hello"))ok(RenderTextPipeline.resolve(h,value,d).equals(value),"unrelated content changed: "+value);
   Script.Hook chat=new Script.Hook(Map.of("id","mock:chat","class","net/minecraft/class_338","method","text","descriptor","(Ljava/lang/String;)Ljava/lang/String;","kind","return","section","messages"));
   ok(RenderTextPipeline.resolve(chat,"玩家：你好",d).equals("玩家：你好"),"chat changed");
   System.out.println("TYPOGRAPHY_COLON_PASS checks="+checks);
  }
 }
}
