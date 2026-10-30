package sao.zh.engine;
import java.lang.reflect.*;import java.util.*;import java.util.function.Function;
/** Scoped semantic labels only. Resolve Text content and resample color runs, retaining all other metadata. */
final class StyledNameProjection {
 private StyledNameProjection(){}
 private static final int MAX_CHARS=1024,MAX_RUNS=128,MAX_LINES=8;
 private record Run(String text,Object style){}
 private static final class Access {
  Class<?> text,visitor;Method getString,visit,literal,setStyle,append,white;Object empty;boolean ready;
  Access(Class<?>owner){try{
   ClassLoader l=owner.getClassLoader();if(l==null)return;
   text=l.loadClass("net.minecraft.class_2561");Class<?> read=l.loadClass("net.minecraft.class_5348"),mutable=l.loadClass("net.minecraft.class_5250"),style=l.loadClass("net.minecraft.class_2583");visitor=l.loadClass("net.minecraft.class_5348$class_5246");
   getString=read.getMethod("getString");visit=read.getMethod("method_27658",visitor,style);literal=text.getMethod("method_43470",String.class);setStyle=mutable.getMethod("method_10862",style);append=mutable.getMethod("method_10852",text);white=style.getMethod("method_36139",int.class);empty=style.getField("field_24360").get(null);ready=true;
  }catch(ReflectiveOperationException|LinkageError ignored){ready=false;}}
 }
 private static final ClassValue<Access> ACCESS=new ClassValue<>(){protected Access computeValue(Class<?>c){return new Access(c);}};
 private static final class Limit extends RuntimeException {}
 static Object apply(Object original,Class<?>owner,Function<String,String>translate)throws Exception {
  if(original==null||original instanceof String||owner==null)return original;
  Access a=ACCESS.get(owner);if(!a.ready||!a.text.isInstance(original))return original;
  String raw=(String)a.getString.invoke(original);
  if(raw==null||raw.isEmpty()||raw.length()>MAX_CHARS||raw.indexOf('§')>=0)return original;
  String[]lines=raw.split("\n",-1);if(lines.length>MAX_LINES)return original;
  String[]mapped=new String[lines.length];boolean different=false;
  for(int i=0;i<lines.length;i++){
   String next=translate.apply(lines[i]);
   mapped[i]=next!=null&&next.length()<=MAX_CHARS&&next.indexOf('\n')<0&&next.indexOf('§')<0?next:lines[i];
   different|=!lines[i].equals(mapped[i]);
  }
  if(!different)return original;
  ArrayList<Run> source=new ArrayList<>();int[]used={0};StringBuilder observed=new StringBuilder();
  Object proxy=Proxy.newProxyInstance(a.visitor.getClassLoader(),new Class<?>[]{a.visitor},(p,m,args)->{
   if(m.getName().equals("accept")){
    String s=(String)args[1];if(!s.isEmpty()){
     if(source.size()>=MAX_RUNS||(used[0]+=s.length())>MAX_CHARS)throw new Limit();
     source.add(new Run(s,args[0]));observed.append(s);
    }
    return Optional.empty();
   }
   if(m.getName().equals("toString"))return "SAO-ZH-Scoped-Text-Visitor";
   if(m.getName().equals("hashCode"))return System.identityHashCode(p);
   if(m.getName().equals("equals"))return p==args[0];
   return null;
  });
  try{a.visit.invoke(original,proxy,a.empty);}catch(InvocationTargetException failure){if(failure.getCause() instanceof Limit)return original;throw failure;}
  if(!raw.contentEquals(observed))return original;
  int[] points=raw.codePoints().toArray();Object[]styles=new Object[points.length];int pos=0;
  for(Run r:source)for(int cp:r.text.codePoints().toArray())styles[pos++]=r.style;
  ArrayList<Run> result=new ArrayList<>();int offset=0;boolean changed=false;
  for(int line=0;line<lines.length;line++){
   int[]old=lines[line].codePoints().toArray(),now=mapped[line].codePoints().toArray();
   int first=0,tail=0;
   while(first<old.length&&first<now.length&&old[first]==now[first])first++;
   while(tail<old.length-first&&tail<now.length-first&&old[old.length-1-tail]==now[now.length-1-tail])tail++;
   int oldCount=old.length-first-tail,newCount=now.length-first-tail;
   boolean replace=oldCount>0&&newCount>0;
   Object metadata=null;
   if(replace)for(int i=first;i<old.length-tail;i++){
    if(Character.getType(old[i])==Character.PRIVATE_USE){replace=false;break;}
    Object stripped=a.white.invoke(styles[offset+i],0xffffff);
    if(metadata==null)metadata=stripped;else if(!metadata.equals(stripped)){replace=false;break;}
   }
   if(replace){
    changed=true;
    for(int i=0;i<first;i++)append(result,old[i],styles[offset+i]);
    for(int i=0;i<newCount;i++){
     int sample=newCount==1?0:(int)((long)i*(oldCount-1)/(newCount-1));
     append(result,now[first+i],styles[offset+first+sample]);
    }
    for(int i=old.length-tail;i<old.length;i++)append(result,old[i],styles[offset+i]);
   }else for(int i=0;i<old.length;i++)append(result,old[i],styles[offset+i]);
   offset+=old.length;
   if(line<lines.length-1){append(result,'\n',styles[offset]);offset++;}
  }
  if(!changed)return original;
  Object output=a.literal.invoke(null,"");
  for(Run run:result){Object child=a.literal.invoke(null,run.text);a.setStyle.invoke(child,run.style);a.append.invoke(output,child);}
  return output;
 }
 private static void append(List<Run>result,int cp,Object style){
  String s=new String(Character.toChars(cp));int n=result.size();
  if(n>0&&result.get(n-1).style.equals(style)){Run last=result.remove(n-1);result.add(new Run(last.text+s,style));}
  else result.add(new Run(s,style));
 }
}
