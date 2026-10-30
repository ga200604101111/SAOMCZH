package sao.zh.compat16;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
/** No native font, GPU, network or running game required. */
public final class GlyphPageStoreContract {
 static int checks;static Class<?> type;
 static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static Object open(Path root,long ram,long disk)throws Exception{return type.getConstructor(Path.class,long.class,long.class).newInstance(root,ram,disk);}
 static Object call(Object s,String name,Class<?>[]types,Object...args)throws Exception{return type.getMethod(name,types).invoke(s,args);}
 static void put(Object s,String ns,int index,int w,int h,byte[]b)throws Exception{call(s,"put",new Class[]{String.class,int.class,int.class,int.class,byte[].class},ns,index,w,h,b);}
 static byte[] get(Object s,String ns,int index,int w,int h)throws Exception{return (byte[])call(s,"get",new Class[]{String.class,int.class,int.class,int.class},ns,index,w,h);}
 static void close(Object s)throws Exception{call(s,"close",new Class[0]);}
 static Map<?,?> stats(Object s)throws Exception{return (Map<?,?>)call(s,"stats",new Class[0]);}
 static byte[] await(Object s,String ns,int idx,int w,int h)throws Exception{
  long until=System.nanoTime()+5_000_000_000L;byte[] b;
  do{b=get(s,ns,idx,w,h);if(b!=null)return b;Thread.sleep(20);}while(System.nanoTime()<until);return null;
 }
 public static void main(String[]a)throws Exception{
  try{type=Class.forName("sao.zh.compat16.GlyphPageStore");}catch(ClassNotFoundException e){throw new AssertionError("Persistent glyph page cache is not implemented",e);}
  Path root=Files.createTempDirectory("glyph-page-contract-");String ns="a".repeat(64),other="b".repeat(64);
  Object s=open(root,32768,131072);byte[]pixels=new byte[256];Arrays.fill(pixels,(byte)57);
  ok(get(s,ns,100,16,16)==null,"Cold glyph unexpectedly present");put(s,ns,100,16,16,pixels);pixels[0]=99;
  byte[] hit=get(s,ns,100,16,16);ok(hit!=null&&hit[0]==57,"Input buffer was retained mutable");hit[1]=88;
  ok(get(s,ns,100,16,16)[1]==57,"Output buffer aliases cached pixels");
  put(s,ns,101,16,16,new byte[256]);ok(get(s,other,100,16,16)==null,"Different font matched");
  ok(get(s,ns,100,32,8)==null,"Wrong dimensions matched");
  try{open(root,32768,131072);throw new AssertionError("Concurrent cache writer accepted");}catch(InvocationTargetException expected){ok(expected.getCause() instanceof java.io.IOException,"Wrong duplicate lock error");}
  put(s,"../../escape",102,16,16,new byte[256]);put(s,ns,103,16,16,new byte[255]);
  ok(get(s,ns,103,16,16)==null,"Invalid byte length accepted");close(s);
  List<Path>pages;try(var list=Files.list(root)){pages=list.filter(p->p.toString().endsWith(".gpage")).toList();}
  ok(pages.size()==1,"Observed neighboring glyphs were not stored in one page: "+pages.size());
  ok(Files.size(pages.get(0))<4096,"Page generated unseen glyphs");
  Object reopened=open(root,32768,131072);call(reopened,"warm",new Class[]{String.class},ns);
  byte[] loaded=await(reopened,ns,100,16,16);ok(loaded!=null&&loaded[0]==57,"Disk cache failed after reopen");
  ok(await(reopened,ns,101,16,16)!=null,"Second page entry missing");
  ok(((Number)stats(reopened).get("disk_reads")).longValue()>0,"Disk hit not observable");close(reopened);
  byte[] corrupt=Files.readAllBytes(pages.get(0));corrupt[corrupt.length-1]^=1;Files.write(pages.get(0),corrupt);
  Object damaged=open(root,32768,131072);call(damaged,"warm",new Class[]{String.class},ns);Thread.sleep(300);
  ok(get(damaged,ns,100,16,16)==null,"Corrupt pixels returned");
  ok(((Number)stats(damaged).get("corrupt_pages")).longValue()>0,"Corrupt page not counted");close(damaged);
  Path bounded=Files.createTempDirectory("glyph-bounds-");Object limited=open(bounded,8192,32768);
  for(int i=0;i<240;i++)put(limited,ns,i*64,32,32,new byte[1024]);Thread.sleep(700);
  ok(((Number)stats(limited).get("memory_bytes")).longValue()<=8192,"Memory cache budget exceeded");close(limited);
  long diskSize;try(var f=Files.list(bounded)){diskSize=f.filter(p->p.toString().endsWith(".gpage")).mapToLong(p->{try{return Files.size(p);}catch(Exception e){throw new RuntimeException(e);}}).sum();}
  ok(diskSize<=32768,"Disk page budget exceeded: "+diskSize);
  ok(get(limited,ns,0,32,32)==null,"Closed cache still active");
  System.out.println("GLYPH_PAGE_STORE_CONTRACT_PASS checks="+checks+" restart=true corruption=true limits=true no_game=true");
 }
}
