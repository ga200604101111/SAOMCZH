package sao.zh.compat16;

import java.io.*;import java.lang.invoke.*;import java.lang.reflect.*;
import java.nio.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.concurrent.atomic.AtomicLong;

/** Bootstrap bridge for the verified native TTF baking call site only.
 * FT_Face remains owned by Minecraft. The IO worker never touches native memory. */
public final class GlyphCacheBridge {
 private record FontState(String namespace,String nativeState){}
 private static final Map<Object,FontState> FACES=Collections.synchronizedMap(new IdentityHashMap<>());
 private static volatile GlyphPageStore store;
 private static final AtomicLong restored=new AtomicLong(),generated=new AtomicLong(),bypassed=new AtomicLong(),failures=new AtomicLong();
 private GlyphCacheBridge(){}
 public static synchronized void configure(Path root,long memory,long disk)throws IOException{
  if(store!=null)throw new IOException("Glyph cache already configured");store=new GlyphPageStore(root,memory,disk);
 }
 public static boolean enabled(){return store!=null;}
 private static String hash(ByteBuffer bytes)throws GeneralSecurityException{MessageDigest d=MessageDigest.getInstance("SHA-256");d.update(bytes.asReadOnlyBuffer());return HexFormat.of().formatHex(d.digest());}
 private static String hash(String value)throws GeneralSecurityException{return hash(ByteBuffer.wrap(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
 private static final class FaceAccess {
  final Method index,flags,size,metrics,xppem,yppem,xscale,yscale,faceGlyph,library,version,getTransform;
  final Method xx,xy,yx,yy,x,y;final ThreadLocal<Object[]> scratch;
  FaceAccess(Class<?>face)throws ReflectiveOperationException{
   ClassLoader loader=face.getClassLoader();Class<?>ft=loader.loadClass("org.lwjgl.util.freetype.FreeType"),matrix=loader.loadClass("org.lwjgl.util.freetype.FT_Matrix"),vector=loader.loadClass("org.lwjgl.util.freetype.FT_Vector");
   index=face.getMethod("face_index");flags=face.getMethod("face_flags");size=face.getMethod("size");metrics=size.getReturnType().getMethod("metrics");Class<?>metric=metrics.getReturnType();
   xppem=metric.getMethod("x_ppem");yppem=metric.getMethod("y_ppem");xscale=metric.getMethod("x_scale");yscale=metric.getMethod("y_scale");
   faceGlyph=face.getMethod("glyph");library=faceGlyph.getReturnType().getMethod("library");version=ft.getMethod("FT_Library_Version",long.class,IntBuffer.class,IntBuffer.class,IntBuffer.class);
   getTransform=ft.getMethod("FT_Get_Transform",face,matrix,vector);xx=matrix.getMethod("xx");xy=matrix.getMethod("xy");yx=matrix.getMethod("yx");yy=matrix.getMethod("yy");x=vector.getMethod("x");y=vector.getMethod("y");
   int ms=matrix.getField("SIZEOF").getInt(null),vs=vector.getField("SIZEOF").getInt(null);Constructor<?> cm=matrix.getConstructor(ByteBuffer.class),cv=vector.getConstructor(ByteBuffer.class);
   scratch=ThreadLocal.withInitial(()->{try{return new Object[]{cm.newInstance(ByteBuffer.allocateDirect(ms).order(ByteOrder.nativeOrder())),cv.newInstance(ByteBuffer.allocateDirect(vs).order(ByteOrder.nativeOrder()))};}catch(Exception e){throw new IllegalStateException(e);}});
  }
  String state(Object face)throws ReflectiveOperationException{
   Object met=metrics.invoke(size.invoke(face));Object[]s=scratch.get();getTransform.invoke(null,face,s[0],s[1]);
   return xppem.invoke(met)+","+yppem.invoke(met)+","+xscale.invoke(met)+","+yscale.invoke(met)+","+xx.invoke(s[0])+","+xy.invoke(s[0])+","+yx.invoke(s[0])+","+yy.invoke(s[0])+","+x.invoke(s[1])+","+y.invoke(s[1]);
  }
  String version(Object face)throws ReflectiveOperationException{
   IntBuffer a=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),b=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),c=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
   version.invoke(null,((Number)library.invoke(faceGlyph.invoke(face))).longValue(),a,b,c);return a.get(0)+"."+b.get(0)+"."+c.get(0);
  }
 }
 private static final ClassValue<FaceAccess> FACE_ACCESS=new ClassValue<>(){protected FaceAccess computeValue(Class<?>c){try{return new FaceAccess(c);}catch(Exception e){throw new IllegalStateException(e);}}};
 /** Called after a supported TrueTypeFont constructor has fixed size and transform. */
 public static void register(ByteBuffer bytes,Object face,float size,float oversample,float sx,float sy){
  GlyphPageStore current=store;if(current==null||bytes==null||face==null||bytes.remaining()>64*1024*1024)return;
  try{
   FaceAccess access=FACE_ACCESS.get(face.getClass());long flags=((Number)access.flags.invoke(face)).longValue();
   // Variable/externally varied faces require a complete axes key; use native behavior for now.
   if((flags&256L)!=0){bypassed.incrementAndGet();return;}
   String state=access.state(face);
   String id=hash("sao-ttf-raster-v1|mc1.21.4|load4|gray8|"+System.getProperty("os.arch")+'|'+access.version(face)+'|'+hash(bytes)+'|'+access.index.invoke(face)+'|'+Float.floatToIntBits(size)+'|'+Float.floatToIntBits(oversample)+'|'+Float.floatToIntBits(sx)+'|'+Float.floatToIntBits(sy)+'|'+state);
   synchronized(FACES){if(FACES.size()>=256){bypassed.incrementAndGet();return;}FACES.put(face,new FontState(id,state));}current.warm(id);
  }catch(Exception|LinkageError e){failures.incrementAndGet();}
 }
 public static void unregister(Object face){if(face!=null)FACES.remove(face);}
 private static final class NativeAccess {
  final MethodHandle render,buffer;final Method width,height,format,components;final Field pointer,allocation;
  NativeAccess(Class<?>image)throws ReflectiveOperationException{
   ClassLoader loader=image.getClassLoader();Class<?>face=loader.loadClass("org.lwjgl.util.freetype.FT_Face"),memory=loader.loadClass("org.lwjgl.system.MemoryUtil");
   render=MethodHandles.publicLookup().unreflect(image.getMethod("method_4316",face,int.class)).asType(MethodType.methodType(boolean.class,Object.class,Object.class,int.class));
   buffer=MethodHandles.publicLookup().unreflect(memory.getMethod("memByteBuffer",long.class,int.class));
   width=image.getMethod("method_4307");height=image.getMethod("method_4323");format=image.getMethod("method_4318");components=format.getReturnType().getMethod("method_4335");
   pointer=image.getDeclaredField("field_4988");pointer.setAccessible(true);allocation=image.getDeclaredField("field_4987");allocation.setAccessible(true);
  }
  ByteBuffer pixels(Object image,int count)throws Throwable{
   if(((Number)components.invoke(format.invoke(image))).intValue()!=1||allocation.getLong(image)<count)return null;
   long p=pointer.getLong(image);return p==0?null:(ByteBuffer)buffer.invokeExact(p,count);
  }
 }
 private static final ClassValue<NativeAccess> IMAGES=new ClassValue<>(){protected NativeAccess computeValue(Class<?>c){try{return new NativeAccess(c);}catch(Exception e){throw new IllegalStateException(e);}}};
 /** Replaces exactly the bake method's NativeImage raster call, NOT global FT_Load_Glyph. */
 public static boolean renderGlyph(Object image,Object face,int glyph)throws Throwable{
  NativeAccess access;
  try{access=IMAGES.get(image.getClass());}
  catch(Exception|LinkageError unsupported){
   try{return (Boolean)image.getClass().getMethod("method_4316",face.getClass(),int.class).invoke(image,face,glyph);}
   catch(InvocationTargetException original){throw original.getCause();}
  }
  GlyphPageStore current=store;FontState font=current==null?null:FACES.get(face);
  int width=0,height=0;ByteBuffer dest=null;boolean eligible=false;
  if(font!=null)try{
   width=((Number)access.width.invoke(image)).intValue();height=((Number)access.height.invoke(image)).intValue();long count=(long)width*height;
   if(width>0&&height>0&&width<=2048&&height<=2048&&count<=256*1024&&font.nativeState.equals(FACE_ACCESS.get(face.getClass()).state(face))){
    dest=access.pixels(image,(int)count);eligible=dest!=null;
    if(eligible){byte[] cached=current.get(font.namespace,glyph,width,height);if(cached!=null){dest.put(cached);restored.incrementAndGet();return true;}}
   }
  }catch(Exception|LinkageError e){failures.incrementAndGet();eligible=false;}
  // Original errors and false return values propagate unchanged; never call it twice.
  boolean ok=(boolean)access.render.invokeExact(image,face,glyph);
  if(ok&&eligible)try{byte[]pixels=new byte[width*height];dest.position(0);dest.get(pixels);current.put(font.namespace,glyph,width,height,pixels);generated.incrementAndGet();}catch(Exception|LinkageError e){failures.incrementAndGet();}
  return ok;
 }
 public static Map<String,Object> stats(){Map<String,Object>m=new LinkedHashMap<>();GlyphPageStore s=store;if(s!=null)m.putAll(s.stats());m.put("restored",restored.get());m.put("generated",generated.get());m.put("bypassed_fonts",bypassed.get());m.put("bridge_failures",failures.get());m.put("registered_fonts",FACES.size());return m;}
 public static synchronized void shutdown(){GlyphPageStore old=store;store=null;FACES.clear();if(old!=null)old.close();}
}
