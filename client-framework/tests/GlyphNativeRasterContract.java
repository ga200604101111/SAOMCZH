package sao.zh.compat16;
import java.io.*;import java.net.*;import java.nio.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.*;
import org.lwjgl.*;import org.lwjgl.system.*;import org.lwjgl.util.freetype.*;
/** Real MC font-provider + NativeImage + real FreeType; no window or OpenGL upload. */
public final class GlyphNativeRasterContract {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static final class Loader extends URLClassLoader {
  Loader(Path mc)throws Exception{super(new URL[]{mc.toUri().toURL()},GlyphNativeRasterContract.class.getClassLoader());}
  protected Class<?> findClass(String name)throws ClassNotFoundException{
   String n=name.replace('.','/');if(!n.equals(GlyphCacheTransformer.FONT)&&!n.equals(GlyphCacheTransformer.BAKER))return super.findClass(name);
   try(InputStream in=findResource(n+".class").openStream()){byte[]b=GlyphCacheTransformer.patch(n,in.readAllBytes());return defineClass(name,b,0,b.length);}catch(Exception e){throw new ClassNotFoundException(name,e);}
  }
 }
 static byte[] bytes(Object image)throws Exception{
  Class<?>c=image.getClass();Field f=c.getDeclaredField("field_4988");f.setAccessible(true);int w=(Integer)c.getMethod("method_4307").invoke(image),h=(Integer)c.getMethod("method_4323").invoke(image);byte[]b=new byte[w*h];MemoryUtil.memByteBuffer(f.getLong(image),b.length).get(b);return b;
 }
 public static void main(String[]args)throws Throwable{
  Path mc=Path.of(args[0]),font=Path.of(args[1]),cache=Path.of(args[2]);boolean warm=args.length>3&&args[3].equals("warm");
  GlyphCacheBridge.configure(cache,32L<<20,256L<<20);long library;
  try(MemoryStack stack=MemoryStack.stackPush()){PointerBuffer p=stack.mallocPointer(1);ok(FreeType.FT_Init_FreeType(p)==0,"FT init");library=p.get(0);}
  try(Loader loader=new Loader(mc)){
   Class<?>provider=loader.loadClass("net.minecraft.class_395"),image=loader.loadClass("net.minecraft.class_1011"),format=loader.loadClass("net.minecraft.class_1011$class_1012");
   loader.loadClass("net.minecraft.class_395$class_397$1").getDeclaredMethods();Object gray=format.getField("field_4998").get(null);
   byte[]source=Files.readAllBytes(font);ByteBuffer data=MemoryUtil.memAlloc(source.length).put(source).flip();FT_Face face;
   try(MemoryStack stack=MemoryStack.stackPush()){PointerBuffer p=stack.mallocPointer(1);ok(FreeType.FT_New_Memory_Face(library,data,0,p)==0,"FT face");face=FT_Face.create(p.get(0));}
   Object nativeFont=provider.getConstructor(ByteBuffer.class,FT_Face.class,float.class,float.class,float.class,float.class,String.class).newInstance(data,face,11f,4f,0f,0f,"");
   ok(((Number)GlyphCacheBridge.stats().get("registered_fonts")).intValue()==1,"Actual font constructor was not registered: "+GlyphCacheBridge.stats());
   if(warm){long end=System.nanoTime()+5_000_000_000L;while(((Number)GlyphCacheBridge.stats().get("disk_reads")).longValue()==0&&System.nanoTime()<end)Thread.sleep(20);ok(((Number)GlyphCacheBridge.stats().get("disk_reads")).longValue()>0,"Restart did not read disk pages");}
   Constructor<?>newImage=image.getConstructor(format,int.class,int.class,boolean.class);Method close=image.getMethod("close");
   for(int cp:new int[]{0x7530,0x6c3e,0x5d84,0x9f98,0x9b31}){
    int glyph=FreeType.FT_Get_Char_Index(face,cp);ok(glyph>0,"Font lacks fixture U+"+Integer.toHexString(cp));
    Object glyphInfo=provider.getMethod("method_2040",int.class).invoke(nativeFont,cp);ok(glyphInfo!=null,"Native glyph metrics absent");
    int w=face.glyph().bitmap().width(),h=face.glyph().bitmap().rows();ok(w>0&&h>0,"Glyph bitmap dimensions absent");
    Object baseline=newImage.newInstance(gray,w,h,false),first=newImage.newInstance(gray,w,h,false),second=newImage.newInstance(gray,w,h,false);
    try{
     ok((Boolean)image.getMethod("method_4316",FT_Face.class,int.class).invoke(baseline,face,glyph),"Original raster failure");byte[]expected=bytes(baseline);
     ok(GlyphCacheBridge.renderGlyph(first,face,glyph),"Cache raster failure");ok(Arrays.equals(expected,bytes(first)),"Native/cached pixels differ U+"+Integer.toHexString(cp));
     long before=((Number)GlyphCacheBridge.stats().get("restored")).longValue();ok(GlyphCacheBridge.renderGlyph(second,face,glyph),"Memory hit failure");
     ok(((Number)GlyphCacheBridge.stats().get("restored")).longValue()>before,"Second raster did not hit cache");ok(Arrays.equals(expected,bytes(second)),"Memory cache pixels differ");
    }finally{close.invoke(baseline);close.invoke(first);close.invoke(second);}
   }
   // Changing the native transform must bypass the old cache namespace.
   long before=((Number)GlyphCacheBridge.stats().get("restored")).longValue();
   try(MemoryStack stack=MemoryStack.stackPush()){FreeType.FT_Set_Transform(face,null,FT_Vector.calloc(stack).x(64).y(0));}
   int glyph=FreeType.FT_Get_Char_Index(face,0x7530);FreeType.FT_Load_Glyph(face,glyph,4);int w=face.glyph().bitmap().width(),h=face.glyph().bitmap().rows();Object shifted=newImage.newInstance(gray,w,h,false);
   try{ok(GlyphCacheBridge.renderGlyph(shifted,face,glyph),"Changed transform native fallback failed");ok(((Number)GlyphCacheBridge.stats().get("restored")).longValue()==before,"Old transformed glyph was reused");}finally{close.invoke(shifted);}
   provider.getMethod("close").invoke(nativeFont);ok(((Number)GlyphCacheBridge.stats().get("registered_fonts")).intValue()==0,"Closed FT face retained");
   System.out.println("NATIVE_TTF_DISK_CACHE_PASS phase="+(warm?"warm":"cold")+" checks="+checks+" pixels_equal=true no_window=true stats="+GlyphCacheBridge.stats());
  }finally{GlyphCacheBridge.shutdown();FreeType.FT_Done_FreeType(library);}
 }
}
