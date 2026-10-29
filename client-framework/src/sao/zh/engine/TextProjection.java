package sao.zh.engine;
import java.lang.reflect.*;import java.util.*;import java.util.function.Function;
/** Display-only projections. Never mutate the original Text, siblings, screen or server model. */
final class TextProjection {
 private static final class Access {
  Method lang,content,plain,siblings,style,copy,literal,setStyle,append;Class<?>plainType;
  Access(Class<?>owner){
   ClassLoader cl=owner.getClassLoader();if(cl==null)return;
   try{lang=cl.loadClass("fr.sao.util.LanguageWatch").getMethod("current");}catch(Exception ignored){}
   try{
    Class<?> t=cl.loadClass("net.minecraft.class_2561"),m=cl.loadClass("net.minecraft.class_5250"),s=cl.loadClass("net.minecraft.class_2583");plainType=cl.loadClass("net.minecraft.class_8828");
    content=t.getMethod("method_10851");plain=plainType.getMethod("comp_737");siblings=t.getMethod("method_10855");style=t.getMethod("method_10866");copy=t.getMethod("method_27662");literal=t.getMethod("method_43470",String.class);setStyle=m.getMethod("method_10862",s);append=m.getMethod("method_10852",t);
   }catch(Exception ignored){plainType=null;}
  }
 }
 private static final ClassValue<Access>access=new ClassValue<>(){protected Access computeValue(Class<?>c){return new Access(c);}};
 static boolean locale(Class<?>owner,String expected){
  if(expected.equals("*"))return true;if(owner==null)return false;try{Access a=access.get(owner);return a.lang!=null&&expected.equalsIgnoreCase(String.valueOf(a.lang.invoke(null)));}catch(Exception ignored){return false;}
 }
 static Object apply(Object original,Class<?>owner,Function<String,String>translate)throws Exception{
  if(original instanceof String)return translate.apply((String)original);
  if(owner==null)return original;Access a=access.get(owner);if(a.plainType==null)return original;return tree(original,a,translate,0,new int[]{0});
 }
 private static Object tree(Object source,Access a,Function<String,String>f,int depth,int[]count)throws Exception{
  if(depth>24||++count[0]>128)return source;Object contents=a.content.invoke(source);
  String raw=a.plainType.isInstance(contents)?(String)a.plain.invoke(contents):null;
  String changed=raw==null?null:f.apply(raw);boolean selfChanged=raw!=null&&!raw.equals(changed);
  List<?>siblings=(List<?>)a.siblings.invoke(source);if(siblings.size()>128)return source;
  // Allocate child copies only when a descendant actually changes. A typical rendered Text
  // does not need an ArrayList allocation on every frame.
  List<Object>children=null;
  for(int i=0;i<siblings.size();i++){
   Object child=siblings.get(i),next=tree(child,a,f,depth+1,count);
   if(children==null&&next!=child){
    children=new ArrayList<>(siblings.size());for(int j=0;j<i;j++)children.add(siblings.get(j));
   }
   if(children!=null)children.add(next);
  }
  if(!selfChanged&&children==null)return source;
  Object result=selfChanged?a.literal.invoke(null,changed):a.copy.invoke(source);
  a.setStyle.invoke(result,a.style.invoke(source));
  for(Object child:children==null?siblings:children)a.append.invoke(result,child);
  return result;
 }
}
