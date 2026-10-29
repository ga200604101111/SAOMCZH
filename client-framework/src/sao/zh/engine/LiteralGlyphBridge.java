package sao.zh.engine;
import java.lang.reflect.Method;
/** Optional bootstrap bridge, resolved once after compatibility installation. */
final class LiteralGlyphBridge {
 private static volatile Method encode,readable;
 private static volatile boolean resolved;
 private LiteralGlyphBridge(){}
 static synchronized void bind(){
  if(resolved)return;resolved=true;
  try{Class<?> c=Class.forName("sao.zh.compat16.LiteralGlyphs",false,null);encode=c.getMethod("encodeTranslated",String.class,String.class);readable=c.getMethod("readable",String.class);}
  catch(ReflectiveOperationException|LinkageError absent){encode=null;readable=null;}
 }
 static String encode(String source,String translated){
  Method m=encode;if(m==null||translated==null)return translated;
  try{return (String)m.invoke(null,source,translated);}catch(ReflectiveOperationException|LinkageError bad){return translated;}
 }
 static String readable(String raw){
  Method m=readable;if(m==null||raw==null)return raw;
  try{return (String)m.invoke(null,raw);}catch(ReflectiveOperationException|LinkageError bad){return raw;}
 }
}
