package sao.zh.compat16;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

/** Early startup text, independent of language/resource reload and native TTF engines. */
public final class SplashBridge {
 private SplashBridge(){}
 private static final Map<String,String> ZH;
 private static final Map<Integer,int[]> GLYPHS;
 private static final int WIDTH,HEIGHT,LINE;
 private static final boolean READY;
 private static boolean warned;
 static {
  Map<String,String> text=Collections.emptyMap();Map<Integer,int[]> glyphs=Collections.emptyMap();
  int width=0,height=0,line=1;boolean ready=false;
  try{
   Map<?,?> lang=(Map<?,?>)MiniJson.parse(new String(resource("zh_tw.json"),StandardCharsets.UTF_8));
   Map<String,String> nextText=new LinkedHashMap<>();
   for(Map.Entry<?,?> e:lang.entrySet())nextText.put((String)e.getKey(),((String)e.getValue()).replace('\uff1a',':'));
   Map<?,?> atlas=(Map<?,?>)MiniJson.parse(new String(resource("atlas.json"),StandardCharsets.UTF_8));
   int nextWidth=number(atlas.get("width")),nextHeight=number(atlas.get("height")),nextLine=number(atlas.get("line_height"));
   if(nextWidth<1||nextWidth>2048||nextHeight<1||nextHeight>2048||nextLine<1||nextText.size()!=53)throw new IOException("Invalid early splash metadata");
   Map<Integer,int[]> nextGlyphs=new HashMap<>();
   for(Map.Entry<?,?> e:((Map<?,?>)atlas.get("glyphs")).entrySet()){
    String key=(String)e.getKey();List<?> list=(List<?>)e.getValue();int[] value=new int[5];
    for(int i=0;i<5;i++)value[i]=number(list.get(i));nextGlyphs.put(key.codePointAt(0),value);
   }
   byte[] png=resource("atlas.png");
   if(png.length<24||(png[0]&255)!=137||png[1]!=80||png[2]!=78||png[3]!=71)throw new IOException("Invalid startup PNG payload");
   text=Collections.unmodifiableMap(nextText);glyphs=Collections.unmodifiableMap(nextGlyphs);
   width=nextWidth;height=nextHeight;line=nextLine;ready=true;
  }catch(Exception|LinkageError failure){
   try{RuntimeBridge.log("STARTUP_ZH_DISABLED_KEEP_ORIGINAL "+failure.getClass().getSimpleName());}catch(Exception|LinkageError ignored){}
  }
  ZH=text;GLYPHS=glyphs;WIDTH=width;HEIGHT=height;LINE=line;READY=ready;
 }
 public static boolean available(){return READY;}
 private static int number(Object value){return ((Number)value).intValue();}
 private static byte[] resource(String name)throws IOException{return SplashAssets.read(name);}
 public static Map<String,String> translations(Map<String,String> original,String locale){
  if(!READY||(!"zh_tw".equalsIgnoreCase(locale)&&!"zh_hk".equalsIgnoreCase(locale)))return original;
  Map<String,String> result=new HashMap<>(original);result.putAll(ZH);return result;
 }
 public static boolean supported(String text){
  if(!READY||text==null)return false;
  for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);if(cp!='\n'&&cp!='\r'&&!GLYPHS.containsKey(cp))return false;}return true;
 }
 private static boolean chinese(String text){return text!=null&&CjkLayout.containsCjk(text)&&supported(text);}
 /** Return -1 to preserve the original renderer's measuring behavior. */
 public static int measure(String text,int height){
  if(text==null||height<=0||!supported(text))return -1;
  long width=0;for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);int[]g=GLYPHS.get(cp);if(g!=null)width+=Math.max(1,Math.round(g[4]*height/(float)LINE));}
  return (int)Math.min(Integer.MAX_VALUE,width);
 }
 public static int measureChinese(String text,int height){return chinese(text)?measure(text,height):-1;}
 public static int fit(String text,int ideal,int max){
  if(!chinese(text))return -1;int h=Math.max(1,ideal);while(h>1&&measure(text,h)>max)h--;return h;
 }
 public static List<String> wrap(String text,int height,int maxWidth){
  if(!chinese(text)||height<=0||maxWidth<=0)return null;
  return CjkLayout.wrap(text,maxWidth,s->measure(s,height));
 }
 private static final ClassValue<DrawAccess> ACCESS=new ClassValue<DrawAccess>(){
  protected DrawAccess computeValue(Class<?> context){try{return new DrawAccess(context);}catch(Exception e){throw new IllegalStateException(e);}}
 };
 private static final class DrawAccess {
  final Class<?> imageType,idType,textureType;final Constructor<?> textureConstructor;final Method imageRead,textureFilter,textureManager,register,draw,renderLayer;
  final Object id;final Function<Object,Object> layer;boolean ready;
  DrawAccess(Class<?> context)throws Exception{
   ClassLoader cl=context.getClassLoader();imageType=cl.loadClass("net.minecraft.class_1011");idType=cl.loadClass("net.minecraft.class_2960");textureType=cl.loadClass("fr.sao.saoui.render.GpuOnlyTexture");
   Method found=null;for(Method m:imageType.getDeclaredMethods())if(Modifier.isStatic(m.getModifiers())&&m.getReturnType()==imageType&&Arrays.equals(m.getParameterTypes(),new Class<?>[]{InputStream.class})){if(found!=null)throw new NoSuchMethodException("Ambiguous PNG reader");found=m;}
   if(found==null)throw new NoSuchMethodException("Native PNG reader absent");imageRead=found;
   textureConstructor=textureType.getConstructor(imageType);textureFilter=textureType.getMethod("method_4527",boolean.class,boolean.class);
   textureManager=cl.loadClass("net.minecraft.class_310").getMethod("method_1531");
   register=cl.loadClass("net.minecraft.class_1060").getMethod("method_4616",idType,cl.loadClass("net.minecraft.class_1044"));
   id=idType.getMethod("method_60655",String.class,String.class).invoke(null,"saotw","startup_zh_v1");
   renderLayer=cl.loadClass("net.minecraft.class_1921").getMethod("method_62275",idType);
   layer=x->{try{return renderLayer.invoke(null,x);}catch(Exception e){throw new IllegalStateException(e);}};
   draw=cl.loadClass("net.minecraft.class_332").getMethod("method_25293",Function.class,idType,int.class,int.class,float.class,float.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class);
  }
  synchronized void ensure(Object client)throws Exception{
   if(ready)return;
   Object image=null,texture=null;boolean transferred=false;
   try{
    try(InputStream in=new ByteArrayInputStream(resource("atlas.png"))){image=imageRead.invoke(null,in);}
    texture=textureConstructor.newInstance(image);transferred=true;
    textureFilter.invoke(texture,true,false);register.invoke(textureManager.invoke(client),id,texture);ready=true;
    RuntimeBridge.log("STARTUP_ZH_TEXTURE_READY "+WIDTH+"x"+HEIGHT+" tips=52");
   }finally{
    if(!ready&&texture!=null)try{textureType.getMethod("close").invoke(texture);}catch(Exception ignored){}
    if(!ready&&!transferred&&image instanceof AutoCloseable)try{((AutoCloseable)image).close();}catch(Exception ignored){}
   }
  }
 }
 public static boolean draw(Object context,Object client,String text,int x,int y,int height,int color,Class<?> owner){
  if(!chinese(text)||height<=0)return false;
  try{
   DrawAccess a=ACCESS.get(owner);a.ensure(client);int at=x;
   for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);int[]g=GLYPHS.get(cp);if(g==null)continue;
    int dest=Math.max(1,Math.round(g[2]*height/(float)LINE));
    a.draw.invoke(context,a.layer,a.id,at,y,(float)g[0],(float)g[1],dest,height,g[2],g[3],WIDTH,HEIGHT,color);
    at+=Math.max(1,Math.round(g[4]*height/(float)LINE));
   }
   return true;
  }catch(Throwable e){if(!warned){warned=true;RuntimeBridge.log("STARTUP_ZH_DRAW_FALLBACK "+e.getClass().getSimpleName());}return false;}
 }
 /** Resolve native methods without allocating textures or starting a game instance. */
 public static void verifyNativeContracts(Class<?> owner)throws Exception{new DrawAccess(owner);}
}
