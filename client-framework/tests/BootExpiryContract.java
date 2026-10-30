package sao.zh.boot;
import java.nio.file.*;import java.util.*;
/** External signed fixtures; never opens network endpoints. */
public final class BootExpiryContract {
 static int checks;
 static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  Path root=Path.of(args[0]);byte[] old=Files.readAllBytes(root.resolve("expired.properties")),sig=Files.readAllBytes(root.resolve("expired.sig"));
  ok(Verified.installedDescriptor(old,sig).get("version").equals("1.2.0"),"Installed descriptor expires");
  try{Verified.descriptor(old,sig);throw new AssertionError("Expired remote offer accepted");}catch(SecurityException expected){checks++;}
  byte[] corrupt=sig.clone();corrupt[0]^=1;
  try{Verified.installedDescriptor(old,corrupt);throw new AssertionError("Bad installed signature accepted");}catch(SecurityException expected){checks++;}
  Path local=root.resolve("slots");Files.createDirectories(local);
  byte[] payload=Files.readAllBytes(root.resolve("module.jar"));String hash=Verified.hash(payload);Files.write(local.resolve(hash+".jar"),payload);
  Files.write(local.resolve("current.properties"),old);Files.write(local.resolve("current.sig"),sig);
  ok(Verified.selectModule(local).get("sha256").equals(hash),"Cannot restore expired local module");
  Files.write(local.resolve("pending.properties"),old);Files.write(local.resolve("pending.sig"),sig);
  ok(Verified.selectModule(local).get("sha256").equals(hash),"Expired incoming candidate broke current");
  ok(Files.exists(local.resolve("pending.properties")),"Expired offer was promoted");
  Files.delete(local.resolve("pending.properties"));Files.delete(local.resolve("pending.sig"));
  Files.copy(local.resolve("current.properties"),local.resolve("previous.properties"),StandardCopyOption.REPLACE_EXISTING);Files.copy(local.resolve("current.sig"),local.resolve("previous.sig"),StandardCopyOption.REPLACE_EXISTING);
  Files.write(local.resolve("current.sig"),corrupt);
  ok(Verified.selectModule(local).get("sha256").equals(hash),"Expired previous module cannot recover");
  payload[0]^=1;Files.write(local.resolve(hash+".jar"),payload);
  try{Verified.selectModule(local);throw new AssertionError("Tampered local module accepted");}catch(Exception expected){checks++;}
  System.out.println("BOOT_EXPIRY_INTEGRATION_PASS checks="+checks+" local_no_expiry=true remote_expiry=true signatures=true no_network=true");
 }
}
