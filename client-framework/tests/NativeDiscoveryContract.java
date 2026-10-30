package sao.zh.engine;
import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;
public final class NativeDiscoveryContract {
 static int checks;static void ok(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{
  Script script=Script.parse(Files.readAllBytes(Path.of(args[0])));String owner="fr/sao/saoui/discovery/DiscoveryOverlayRenderer";
  Script.Hook h=script.byId.get("worlds-locations:discovery-title");ok(h!=null,"Missing discovery hook");ok(SemanticText.location(h),"Not shared location route");
  byte[]raw;try(ZipFile z=new ZipFile(args[1])){raw=z.getInputStream(z.getEntry(owner+".class")).readAllBytes();}
  byte[]patched=new RuleTransformer().patch(owner,raw,List.of(h));List<String>events=new ArrayList<>();int[]iconReads={0};
  new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){if(!n.equals("onHudRender"))return null;return new MethodVisitor(Opcodes.ASM9){
   public void visitFieldInsn(int op,String o,String n,String d){events.add("field:"+n);if(n.equals("ICON_TEX"))iconReads[0]++;}
   public void visitMethodInsn(int op,String o,String n,String d,boolean i){events.add(o+"."+n);}
  };}},ClassReader.SKIP_DEBUG);
  int source=events.indexOf("field:titleText"),dispatch=events.indexOf("sao/zh/api/Dispatch.value"),width=events.indexOf("fr/sao/font/TextUtil.width"),draw=events.indexOf("fr/sao/font/TextUtil.drawText");
  ok(source>=0&&dispatch==source+1,"Projection not immediately after title field");ok(dispatch<width&&width<draw,"Translation must precede both native width and draw");ok(Collections.frequency(events,"sao/zh/api/Dispatch.value")==1,"Other consumers changed");ok(iconReads[0]==1,"Emblem image changed");
  Map<String,Map<String,String>>dict=Map.of("lines",Map.of("HANAKA","哈納卡"));ok(RenderTextPipeline.resolve(h,"HANAKA",dict).equals("哈納卡"),"Not reusing names/lines dictionary");ok(RenderTextPipeline.styledName(h),"Styled text path missing");
  System.out.println("NATIVE_DISCOVERY_PASS checks="+checks+" native_bytecode=true before_width=true original_icon=true shared_dictionary=true");
 }
}
