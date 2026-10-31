package sao.zh.engine;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
/** Native Java Agent only: exact Sound of Aincrad Path-return hook and independent HTML installer. */
public final class NativeWebUiAgentContract {
    private static int checks;
    private static void ok(boolean x,String message){checks++;if(!x)throw new AssertionError(message);}
    private static Map<String,Object> spec(){
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id","zapaudio-webui:extracted-html");
        m.put("class","fr/saofr/zapaudio/client/web/WebClientExtractor");
        m.put("method","extract");
        m.put("descriptor","()Ljava/nio/file/Path;");
        m.put("kind","web_extract");m.put("section","lines");
        return m;
    }
    private static void mustReject(String field,Object value){
        Map<String,Object> changed=spec();changed.put(field,value);
        try{new Script.Hook(changed);}catch(IllegalArgumentException expected){checks++;return;}
        throw new AssertionError("Unexpected Path hook authorization: "+field);
    }
    public static void main(String[] args)throws Exception {
        Script.Hook h=new Script.Hook(spec());
        ok(h.id.equals("zapaudio-webui:extracted-html"),"Allowlisted id mismatch");
        for(String owner:new String[]{"net/minecraft/class_310","java/nio/file/Files","fr/saofr/zapaudio/client/web/WrongExtractor"})
            mustReject("class",owner);
        mustReject("method","rewriteAudioUrl");
        mustReject("descriptor","(Ljava/lang/String;)Ljava/nio/file/Path;");
        mustReject("descriptor","()Ljava/lang/String;");
        mustReject("kind","return");
        mustReject("section","messages");
        mustReject("capture","world");
        String target="fr/saofr/zapaudio/client/web/WebClientExtractor";
        byte[] raw;
        try(ZipFile z=new ZipFile(args[0])){raw=z.getInputStream(z.getEntry(target+".class")).readAllBytes();}
        byte[] patched=new RuleTransformer().patch(target,raw,List.of(h));
        ok(patched.length>raw.length,"Bytecode not instrumented");
        final int[] exits={0},calls={0},mods={0};
        new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM9){
            @Override public MethodVisitor visitMethod(int access,String n,String desc,String signature,String[]errors){
                if(!n.equals("extract")||!desc.equals("()Ljava/nio/file/Path;"))return null;
                mods[0]=access;
                return new MethodVisitor(Opcodes.ASM9){
                    @Override public void visitMethodInsn(int op,String owner,String name,String d,boolean itf){
                        if(owner.equals("sao/zh/api/Dispatch")&&name.equals("value"))calls[0]++;
                    }
                    @Override public void visitInsn(int op){if(op==Opcodes.ARETURN)exits[0]++;}
                };
            }
        },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        ok((mods[0]&Opcodes.ACC_STATIC)!=0&& (mods[0]&Opcodes.ACC_SYNCHRONIZED)!=0,
          "Expected synchronized static extractor lost");
        ok(exits[0]>=2,"Original extractor should have cache and normal returns");
        ok(calls[0]==exits[0],"All native Path return paths must call the Agent exactly once");
        // ABI integrity: native target is transformed without class field/method additions.
        final Set<String> before=new TreeSet<>(),after=new TreeSet<>();
        for(var pair:List.of(Map.entry(raw,before),Map.entry(patched,after))){
            new ClassReader(pair.getKey()).accept(new ClassVisitor(Opcodes.ASM9){
                @Override public FieldVisitor visitField(int a,String n,String d,String s,Object v){pair.getValue().add("F "+n+" "+d);return null;}
                @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){pair.getValue().add("M "+n+" "+d);return null;}
            },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }
        ok(before.equals(after),"Agent changed extractor public ABI");
        System.out.println("NATIVE_ZAPAUDIO_AGENT_PATH_HOOK_PASS checks="+checks+
         " exits="+exits[0]+" actual_target=true no_mod_installed=true");
    }
}
