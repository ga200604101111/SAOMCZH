package sao.zh.engine;
import java.lang.reflect.*;import java.nio.charset.StandardCharsets;
/** Installed content is not an expiring update offer; new remote rules remain fresh-only. */
public final class InstalledRulesExpiryContract {
 public static void main(String[] args)throws Exception {
  byte[] old="{\"schema_version\":2,\"version\":\"1.6.0\",\"sequence\":108,\"expires\":1,\"hooks\":[]}".getBytes(StandardCharsets.UTF_8);
  try{Script.parse(old);throw new AssertionError("Expired incoming rules accepted");}catch(IllegalArgumentException expected){}
  Method parse;
  try{parse=Script.class.getDeclaredMethod("parseInstalled",byte[].class);}catch(NoSuchMethodException absent){throw new AssertionError("Installed signed rules still expire",absent);}
  parse.setAccessible(true);Script s=(Script)parse.invoke(null,(Object)old);
  if(!s.version.equals("1.6.0")||s.sequence!=108)throw new AssertionError("Installed rule identity changed");
  byte[] malformed=new String(old,StandardCharsets.UTF_8).replace("\"hooks\":[]","\"hooks\":null").getBytes(StandardCharsets.UTF_8);
  try{parse.invoke(null,(Object)malformed);throw new AssertionError("Malformed installed rules accepted");}catch(InvocationTargetException e){if(!(e.getCause() instanceof IllegalArgumentException))throw e;}
  System.out.println("INSTALLED_RULE_EXPIRY_CONTRACT_PASS incoming_expired_rejected=true installed_validated=true malformed_rejected=true");
 }
}
