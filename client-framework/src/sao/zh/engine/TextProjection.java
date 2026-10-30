package sao.zh.engine;
import java.lang.reflect.*;import java.util.*;import java.util.function.Function;
/** Display-only projections. Never mutate the original Text, siblings, screen or server model. */
final class TextProjection {
 private static final class Access {
  Method lang,content,plain,siblings,style,copy,literal,setStyle,append,withParent,getString;Class<?>plainType;
  Access(Class<?>owner){
   ClassLoader cl=owner.getClassLoader();if(cl==null)return;
   try{lang=cl.loadClass("fr.sao.util.LanguageWatch").getMethod("current");}catch(Exception ignored){}
   try{
    Class<?> t=cl.loadClass("net.minecraft.class_2561"),m=cl.loadClass("net.minecraft.class_5250"),s=cl.loadClass("net.minecraft.class_2583");plainType=cl.loadClass("net.minecraft.class_8828");
    content=t.getMethod("method_10851");plain=plainType.getMethod("comp_737");siblings=t.getMethod("method_10855");style=t.getMethod("method_10866");copy=t.getMethod("method_27662");literal=t.getMethod("method_43470",String.class);setStyle=m.getMethod("method_10862",s);append=m.getMethod("method_10852",t);
    try{withParent=s.getMethod("method_27702",s);}catch(NoSuchMethodException optional){}
    try{getString=t.getMethod("getString");}catch(NoSuchMethodException optional){}
   }catch(Exception ignored){plainType=null;}
  }
 }
 private static final ClassValue<Access>access=new ClassValue<>(){protected Access computeValue(Class<?>c){return new Access(c);}};
 static boolean locale(Class<?>owner,String expected){
  if(expected.equals("*"))return true;if(owner==null)return false;try{Access a=access.get(owner);return a.lang!=null&&expected.equalsIgnoreCase(String.valueOf(a.lang.invoke(null)));}catch(Exception ignored){return false;}
 }
 static String plainText(Object original,Class<?>owner)throws Exception{
  if(original==null)return null;if(original instanceof String)return (String)original;
  if(owner==null)return null;Access a=access.get(owner);if(a.getString!=null)return (String)a.getString.invoke(original);
  StringBuilder b=new StringBuilder();return collectPlain(original,a,b,0,new int[]{0})?b.toString():null;
 }
 private static boolean collectPlain(Object node,Access a,StringBuilder out,int depth,int[]count)throws Exception{
  if(a.plainType==null||depth>24||++count[0]>128)return false;Object c=a.content.invoke(node);
  if(!a.plainType.isInstance(c))return false;out.append((String)a.plain.invoke(c));if(out.length()>8192)return false;
  for(Object child:(List<?>)a.siblings.invoke(node))if(!collectPlain(child,a,out,depth+1,count))return false;return true;
 }
 static Object apply(Object original,Class<?>owner,Function<String,String>translate)throws Exception{
  return apply(original,owner,translate,false);
 }
 static Object apply(Object original,Class<?>owner,Function<String,String>translate,boolean wholeFirst)throws Exception{
  if(original==null)return null;
  if(original instanceof String)return translate.apply((String)original);
  if(owner==null)return original;Access a=access.get(owner);if(a.plainType==null)return original;
  // Only approved domain call sites pay the cost of full-text reconstruction.
  // Reassemble BEFORE the native renderer splits/measures; leave the source untouched.
  if(wholeFirst){
   Uniform full=uniformLiteral(original,a);
   if(full!=null){String mapped=translate.apply(full.text);
    if(mapped!=null&&!mapped.equals(full.text)&&mapped.length()<=16384){
     Object copy=a.literal.invoke(null,mapped);a.setStyle.invoke(copy,full.style);return copy;
    }
   }
  }
  return tree(original,a,translate,0,new int[]{0});
 }
 private record Uniform(String text,Object style){}
 private static Uniform uniformLiteral(Object root,Access a)throws Exception{
  List<?>children=(List<?>)a.siblings.invoke(root);if(children.isEmpty()||children.size()>128)return null;
  StringBuilder out=new StringBuilder();Object[]chosen={null};
  return appendUniform(root,a,null,chosen,out,0,new int[]{0})&&chosen[0]!=null?new Uniform(out.toString(),chosen[0]):null;
 }
 private static boolean appendUniform(Object node,Access a,Object inherited,Object[]chosen,StringBuilder out,int depth,int[]count)throws Exception{
  if(depth>24||++count[0]>128)return false;
  Object style=a.style.invoke(node);if(inherited!=null&&a.withParent!=null)style=a.withParent.invoke(style,inherited);
  Object content=a.content.invoke(node);if(!a.plainType.isInstance(content))return false;
  String part=(String)a.plain.invoke(content);
  if(!part.isEmpty()){
   if(chosen[0]==null)chosen[0]=style;else if(!Objects.equals(chosen[0],style))return false;
   for(int at=0;at<part.length();){int cp=part.codePointAt(at);at+=Character.charCount(cp);if(cp==0xa7||Character.getType(cp)==Character.PRIVATE_USE)return false;}
  }
  if(out.length()+part.length()>8192)return false;out.append(part);
  List<?>children=(List<?>)a.siblings.invoke(node);if(children.size()>128)return false;
  for(Object child:children)if(!appendUniform(child,a,style,chosen,out,depth+1,count))return false;return true;
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
