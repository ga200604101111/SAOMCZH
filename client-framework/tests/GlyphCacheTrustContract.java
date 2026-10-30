package sao.zh.compat16;
import java.nio.file.*;import java.util.*;import sao.zh.boot.Verified;
/** Operates on an isolated descriptor fixture, never on the active extension root. */
public final class GlyphCacheTrustContract {
 public static void main(String[]args)throws Exception{
  Path root=Path.of(args[0]);byte[]body=Files.readAllBytes(root.resolve("pending.properties")),sig=Files.readAllBytes(root.resolve("pending.sig"));
  Map<String,String>spec=Verified.descriptor(body,sig);if(!spec.get("local_only").equals("true"))throw new AssertionError("Missing local-only boundary");
  byte[]bad=sig.clone();bad[0]^=1;try{Verified.descriptor(body,bad);throw new AssertionError("Bad signature accepted");}catch(SecurityException expected){}
  Map<String,String>chosen=Verified.selectModule(root);if(!chosen.get("sha256").equals(spec.get("sha256")))throw new AssertionError("Signed trial not selected");
  Path file=root.resolve(spec.get("sha256")+".jar"),hidden=file.resolveSibling(file.getFileName()+".hidden");Files.move(file,hidden);
  try{Map<String,String>fallback=Verified.selectModule(root);if(fallback.get("sha256").equals(spec.get("sha256")))throw new AssertionError("Missing local binary did not recover");}
  finally{Files.move(hidden,file);}
  System.out.println("GLYPH_TRIAL_EXISTING_KERNEL_TRUST_PASS checks=4 signed=true recovery=true no_network=true");
 }
}
