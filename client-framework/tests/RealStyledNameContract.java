package sao.zh.engine;
import java.lang.reflect.*;import java.util.*;import java.util.function.Function;
/** Runs against the real intermediary Minecraft Text/Style implementation without Minecraft startup or GL. */
public final class RealStyledNameContract {
 static int checks;static void ok(boolean c,String m){checks++;if(!c)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  Class<?> text=Class.forName("net.minecraft.class_2561"),mutable=Class.forName("net.minecraft.class_5250"),style=Class.forName("net.minecraft.class_2583"),read=Class.forName("net.minecraft.class_5348");
  Method literal=text.getMethod("method_43470",String.class),translatable=text.getMethod("method_43471",String.class),append=mutable.getMethod("method_10852",text),set=mutable.getMethod("method_10862",style),color=style.getMethod("method_36139",int.class),bold=style.getMethod("method_10982",Boolean.class),string=read.getMethod("getString");Object empty=style.getField("field_24360").get(null);
  Function<String,String> f=s->Map.of("Shard Keeper","碎片管理員","Hanaka","哈納卡","Right-click: your daily shards.","右鍵領取每日碎片。").getOrDefault(s,s);
  Object name=literal.invoke(null,"");int index=0;for(char c:"Shard Keeper".toCharArray()){Object part=literal.invoke(null,String.valueOf(c));set.invoke(part,color.invoke(empty,index++*1234));append.invoke(name,part);}
  Object translated=StyledNameProjection.apply(name,RealStyledNameContract.class,f);ok(translated!=name,"Real gradient was not translated");ok(string.invoke(translated).equals("碎片管理員"),"Wrong real gradient translation");ok(string.invoke(name).equals("Shard Keeper"),"Real source mutated");ok(mutable.isInstance(translated),"Wrong native Text return type");
  Object dynamic=translatable.invoke(null,"Hanaka");Object shown=StyledNameProjection.apply(dynamic,RealStyledNameContract.class,f);ok(string.invoke(shown).equals("哈納卡"),"Actual translatable content was missed");ok(dynamic!=shown,"Resolved text unchanged");
  Object multiline=literal.invoke(null,"");append.invoke(multiline,name);append.invoke(multiline,literal.invoke(null,"\n"));Object prompt=literal.invoke(null,"Right-click: your daily shards.");set.invoke(prompt,bold.invoke(empty,Boolean.TRUE));append.invoke(multiline,prompt);Object two=StyledNameProjection.apply(multiline,RealStyledNameContract.class,f);ok(string.invoke(two).equals("碎片管理員\n右鍵領取每日碎片。"),"Mixed-metadata multiline labels not translated independently");
  Object blocked=literal.invoke(null,"");append.invoke(blocked,literal.invoke(null,"Shard "));Object end=literal.invoke(null,"Keeper");set.invoke(end,bold.invoke(empty,Boolean.TRUE));append.invoke(blocked,end);ok(StyledNameProjection.apply(blocked,RealStyledNameContract.class,f)==blocked,"Mixed noncolor metadata must fail open");
  Object unrelated=literal.invoke(null,"Not in the reviewed dictionary");ok(StyledNameProjection.apply(unrelated,RealStyledNameContract.class,f)==unrelated,"Unknown rebuilt");
  System.out.println("REAL_MINECRAFT_TEXT_CONTRACT_PASS checks="+checks+" gradient=true translatable=true native_types=true no_game=true");
 }
}
