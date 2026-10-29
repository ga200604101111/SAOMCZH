package sao.zh.compat16;
import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.io.*;import java.lang.reflect.*;
/** Contract for all font-manifest aliases, not a handpicked regression character list. */
public final class LiteralGlyphsContract {
 static int checks;
 static void ok(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
 public static void main(String[]args)throws Exception{
  Class<?> policy;
  try{policy=Class.forName("sao.zh.compat16.LiteralGlyphs");}
  catch(ClassNotFoundException absent){throw new AssertionError("Missing corpus-wide literal/icon glyph isolation",absent);}
  Method configure=policy.getMethod("configure",Path.class),encode=policy.getMethod("encodeTranslated",String.class,String.class),decode=policy.getMethod("readable",String.class),isAlias=policy.getMethod("isAlias",int.class);
  Path pack=Path.of(args[0]);Map<?,?> doc;
  try(ZipFile z=new ZipFile(pack.toFile());InputStream in=z.getInputStream(z.getEntry("assets/saotw/glyph-routing.json"))){doc=(Map<?,?>)MiniJson.parse(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
  Map<?,?> aliases=(Map<?,?>)doc.get("aliases");int count=((Number)configure.invoke(null,pack)).intValue();ok(count==aliases.size()&&count>=30,"Incomplete alias inventory");
  int iconInventory=FontColorPolicy.configure(pack,args.length>1?Path.of(args[1]):null,null);
  ok(iconInventory>=30,"Original bitmap icon inventory lost");
  ok(!FontColorPolicy.correct(true,'氾')&&!FontColorPolicy.correct(true,'嶄'),"Normal Han still treated as colored icon");
  ok(FontColorPolicy.correct(true,'田'),"Original server banner tint semantics changed");
  for(Map.Entry<?,?> e:aliases.entrySet()){
   int original=Integer.parseInt((String)e.getKey()),alias=((Number)e.getValue()).intValue();String han=new String(Character.toChars(original)),a=new String(Character.toChars(alias));
   ok(encode.invoke(null,"English source",han).equals(a),"Literal mapping missing "+original);
   ok(decode.invoke(null,a).equals(han),"Readable roundtrip failed "+original);
   ok(Boolean.TRUE.equals(isAlias.invoke(null,alias)),"Alias absent "+alias);
   ok(!Boolean.TRUE.equals(isAlias.invoke(null,original)),"Original icon is marked as text alias");
   ok(encode.invoke(null,han,han).equals(han),"Unchanged original icon rewritten");
   ok(encode.invoke(null,han+" English",han+" 中文內容").toString().startsWith(han),"Inherited source icon rewritten");
   ok(encode.invoke(null,"English",a).equals(a),"Alias double-encoded");
   ok(!FontColorPolicy.correct(true,alias),"Text alias lost text coloring");
  }
  ok(encode.invoke(null,"Player_123","Player_123").equals("Player_123"),"Player ID changed");
  ok(encode.invoke(null,"raw","圖示 \uea72 10% +5").toString().contains("\uea72 10% +5"),"Unrelated icon or numeric formatting changed");
  ok(encode.invoke(null,null,null)==null,"Null input");
  ok(encode.invoke(null,"same","same").equals("same"),"Untranslated text changed");
  String title=encode.invoke(null,"19. The Chamber Beneath the Fields","19. 田野下的密室").toString();
  ok(!title.contains("田")&&decode.invoke(null,title).equals("19. 田野下的密室"),"Quest title still contains banner codepoint");
  // Both old compatibility translation and new engine use the same display mapping.
  Field dictionary=RuntimeBridge.class.getDeclaredField("dictionary");dictionary.setAccessible(true);
  Field enabled=RuntimeBridge.class.getDeclaredField("translationEnabled");enabled.setAccessible(true);enabled.setBoolean(null,true);
  dictionary.set(null,Map.of("lines",Map.of("Quest text","田野中的密室")));
  ok(decode.invoke(null,RuntimeBridge.lookup("Quest text","lines","zh_tw")).equals("田野中的密室"),"Legacy bridge lost text semantics");
  ok(!RuntimeBridge.lookup("Quest text","lines","zh_tw").contains("田"),"Legacy bridge still emits colliding glyph");
  ok(RuntimeBridge.lookup("Quest text","lines","en_us").equals("Quest text"),"Locale boundary lost");
  Path empty=Files.createTempFile("no-glyph-contract-",".zip");
  try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(empty))){z.putNextEntry(new ZipEntry("pack.mcmeta"));z.write("{}".getBytes());z.closeEntry();}
  try{ok(((Number)configure.invoke(null,empty)).intValue()==0,"Missing pack should disable extra aliases");ok(encode.invoke(null,"English","田野").equals("田野"),"New aliases emitted without matching assets");}finally{Files.deleteIfExists(empty);}
  System.out.println("LITERAL_GLYPH_CONTRACT_PASS checks="+checks+" aliases="+count+" no_game=true");
 }
}
