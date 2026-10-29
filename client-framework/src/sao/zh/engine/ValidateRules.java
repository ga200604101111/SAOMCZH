package sao.zh.engine;
import java.nio.file.*;import java.util.*;import java.util.jar.*;
/** Maintainer validation only. No transformer installation and no target class loading. */
public final class ValidateRules {
 public static void main(String[]args)throws Exception{
  if(args.length<1)throw new IllegalArgumentException("Usage: ValidateRules rules.json [mod.jar ...]");
  Path file=Paths.get(args[0]);if(Files.size(file)>Script.LIMIT)throw new IllegalArgumentException("Rule size limit");
  Script spec=Script.parse(Files.readAllBytes(file));Set<String>pending=new HashSet<>(spec.byClass.keySet());RuleTransformer transformer=new RuleTransformer();
  for(int i=1;i<args.length;i++)try(JarFile jar=new JarFile(args[i])){
   for(String owner:new ArrayList<>(pending)){JarEntry entry=jar.getJarEntry(owner+".class");if(entry!=null){byte[]input;try(java.io.InputStream in=jar.getInputStream(entry)){input=in.readAllBytes();}transformer.patch(owner,input,spec.byClass.get(owner));pending.remove(owner);}}
  }
  if(args.length>1&&!pending.isEmpty())throw new IllegalArgumentException("Targets absent from provided mod JARs: "+pending);
  System.out.println("EXTENSION_RULE_VALIDATION_PASS version="+spec.version+" sequence="+spec.sequence+" hooks="+spec.byId.size()+" targets="+spec.byClass.size()+" checked_bytecode="+(args.length>1));
 }
}
