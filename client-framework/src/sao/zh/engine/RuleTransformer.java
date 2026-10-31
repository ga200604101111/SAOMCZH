package sao.zh.engine;
import java.lang.instrument.*;import java.security.ProtectionDomain;import java.util.*;import java.util.concurrent.ConcurrentLinkedQueue;import java.util.concurrent.atomic.AtomicLong;import org.objectweb.asm.*;
/** Adds method-body projections only; cannot add fields/methods or change class hierarchy. */
final class RuleTransformer implements ClassFileTransformer {
 volatile Script plan=Script.empty();final Queue<String>errors=new ConcurrentLinkedQueue<>();final AtomicLong count=new AtomicLong();
 public byte[] transform(ClassLoader l,String name,Class<?>redef,ProtectionDomain pd,byte[]bytes){
  List<Script.Hook>hooks=plan.byClass.get(name);if(hooks==null||hooks.isEmpty())return null;
  try{byte[]out=patch(name,bytes,hooks);count.incrementAndGet();return out;}catch(Throwable bad){if(errors.size()<128)errors.add(name+": "+bad.getClass().getSimpleName()+" "+String.valueOf(bad.getMessage()));return null;}
 }
 byte[] patch(String cls,byte[]bytes,List<Script.Hook>hooks){
  ClassReader input=new ClassReader(bytes);if(!cls.equals(input.getClassName()))throw new IllegalArgumentException("Class mismatch");
  Map<String,String>fields=new HashMap<>();Map<String,Integer>fieldFlags=new HashMap<>(),methodFlags=new HashMap<>();
  input.accept(new ClassVisitor(Opcodes.ASM9){
   public FieldVisitor visitField(int a,String n,String d,String sig,Object v){fields.put(n,d);fieldFlags.put(n,a);return null;}
   public MethodVisitor visitMethod(int a,String n,String d,String sig,String[]e){methodFlags.put(n+d,a);return null;}
  },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
  for(Script.Hook h:hooks){
   if(h.kind.equals("web_extract")){
    Integer flags=methodFlags.get(h.method+h.descriptor);
    if(flags==null||(flags&Opcodes.ACC_STATIC)==0||(flags&Opcodes.ACC_SYNCHRONIZED)==0)
     throw new IllegalArgumentException("Sound of Aincrad extractor must be synchronized and static");
   }
   if(h.kind.equals("description_list")){
    Integer flags=methodFlags.get(h.method+h.descriptor);if(flags==null||(flags&Opcodes.ACC_STATIC)!=0)throw new IllegalArgumentException("Description accessor must be an instance method");
   }
   if(h.kind.equals("refresh")||h.kind.equals("wrap")){
    Integer flags=methodFlags.get(h.method+h.descriptor);if(flags==null||(flags&Opcodes.ACC_STATIC)!=0)throw new IllegalArgumentException("Layout target must be an instance method");
    for(Map.Entry<String,String>e:h.contextFields.entrySet())if(!e.getValue().equals(fields.get(e.getKey()))||(fieldFlags.get(e.getKey())&Opcodes.ACC_STATIC)!=0)throw new IllegalArgumentException("Context field mismatch "+e.getKey());
    for(Script.CacheField f:h.cacheFields){Integer ff=fieldFlags.get(f.name);if(!f.descriptor.equals(fields.get(f.name))||ff==null||(ff&Opcodes.ACC_STATIC)!=0||f.action.equals("reset_key")&&(ff&Opcodes.ACC_FINAL)!=0)throw new IllegalArgumentException("Display cache field mismatch "+f.name);}
    if(h.kind.equals("wrap")){Integer mf=methodFlags.get(h.measure+"(Ljava/lang/String;FZ)F");if(mf==null||(mf&Opcodes.ACC_STATIC)!=0||(mf&Opcodes.ACC_PUBLIC)==0)throw new IllegalArgumentException("Public native-width measurement missing");}
   }
  }
  ClassWriter writer=new ClassWriter(input,ClassWriter.COMPUTE_MAXS);Map<String,Integer>found=new HashMap<>();
  input.accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[]exceptions){
    MethodVisitor original=super.visitMethod(access,name,desc,sig,exceptions);
    List<Script.Hook>selected=new ArrayList<>();for(Script.Hook h:hooks)if(h.method.equals(name)&&h.descriptor.equals(desc))selected.add(h);
    if(selected.isEmpty())return original;if((access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT))!=0)throw new IllegalArgumentException("Method has no transformable body");
    return new MethodVisitor(Opcodes.ASM9,original){
     void dispatch(Script.Hook h,String valueDescriptor){
      super.visitLdcInsn(h.id);super.visitInsn(Opcodes.SWAP);super.visitLdcInsn(Type.getObjectType(cls));
      if(h.kind.equals("description_list")){
       super.visitMethodInsn(Opcodes.INVOKESTATIC,"sao/zh/api/Dispatch","description","(Ljava/lang/String;Ljava/util/List;Ljava/lang/Class;)Ljava/util/List;",false);found.merge(h.id,1,Integer::sum);return;
      }
      boolean string=valueDescriptor.equals(Script.STR);
      super.visitMethodInsn(Opcodes.INVOKESTATIC,"sao/zh/api/Dispatch",string?"text":"value",string?"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/String;":"(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;",false);
      if(!string)super.visitTypeInsn(Opcodes.CHECKCAST,Type.getType(valueDescriptor).getInternalName());found.merge(h.id,1,Integer::sum);
     }
     @Override public void visitCode(){super.visitCode();
      for(Script.Hook h:selected)if(h.kind.equals("refresh")){super.visitLdcInsn(h.id);super.visitVarInsn(Opcodes.ALOAD,0);super.visitLdcInsn(Type.getObjectType(cls));super.visitMethodInsn(Opcodes.INVOKESTATIC,"sao/zh/api/Dispatch","refresh","(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;)V",false);found.merge(h.id,1,Integer::sum);}
      for(Script.Hook h:selected)if(h.kind.equals("argument")){
      Type[]args=Type.getArgumentTypes(desc);int slot=(access&Opcodes.ACC_STATIC)==0?1:0;for(int i=0;i<h.argument;i++)slot+=args[i].getSize();
      super.visitVarInsn(Opcodes.ALOAD,slot);dispatch(h,args[h.argument].getDescriptor());super.visitVarInsn(Opcodes.ASTORE,slot);
     }}
     @Override public void visitInsn(int op){
      if(op==Opcodes.ARETURN)for(Script.Hook h:selected){
       if(h.kind.equals("return")||h.kind.equals("description_list")||h.kind.equals("web_extract"))dispatch(h,Type.getReturnType(desc).getDescriptor());
       if(h.kind.equals("mob_name")){
        if((access&Opcodes.ACC_STATIC)==0)throw new IllegalArgumentException("Mob shownName must be static");
        super.visitLdcInsn(h.id);super.visitInsn(Opcodes.SWAP);
        super.visitVarInsn(Opcodes.ALOAD,0);super.visitFieldInsn(Opcodes.GETFIELD,"fr/sao/saoui/mob/MobBarEntry","name","Ljava/lang/String;");
        super.visitLdcInsn(Type.getObjectType(cls));super.visitVarInsn(Opcodes.ALOAD,0);super.visitFieldInsn(Opcodes.GETFIELD,"fr/sao/saoui/mob/MobBarEntry","player","Z");
        super.visitMethodInsn(Opcodes.INVOKESTATIC,"sao/zh/api/Dispatch","mobText","(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Z)Ljava/lang/String;",false);found.merge(h.id,1,Integer::sum);
       }
       if(h.kind.equals("wrap")){super.visitLdcInsn(h.id);super.visitVarInsn(Opcodes.ALOAD,0);super.visitLdcInsn(Type.getObjectType(cls));super.visitVarInsn(Opcodes.ALOAD,1);super.visitVarInsn(Opcodes.FLOAD,2);super.visitVarInsn(Opcodes.ILOAD,3);super.visitVarInsn(Opcodes.FLOAD,4);super.visitVarInsn(Opcodes.ILOAD,5);super.visitMethodInsn(Opcodes.INVOKESTATIC,"sao/zh/api/Dispatch","wrap","(Ljava/util/List;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;FZFI)Ljava/util/List;",false);found.merge(h.id,1,Integer::sum);}
      }
      if(op==Opcodes.RETURN)for(Script.Hook h:selected)if(h.kind.equals("armorstand_nameplate")){
       // Only the ArmorStand-specific client render-state method, never the entity/server data.
       // After superclass extraction, its nameplate Text is the superclass render-state field.
       super.visitVarInsn(Opcodes.ALOAD,2);
       super.visitInsn(Opcodes.DUP);
       super.visitFieldInsn(Opcodes.GETFIELD,"net/minecraft/class_10017","field_53337",Script.TEXT);
       dispatch(h,Script.TEXT);
       super.visitFieldInsn(Opcodes.PUTFIELD,"net/minecraft/class_10017","field_53337",Script.TEXT);
      }
      super.visitInsn(op);
     }
     @Override public void visitFieldInsn(int op,String owner,String field,String d){super.visitFieldInsn(op,owner,field,d);if(op==Opcodes.GETFIELD||op==Opcodes.GETSTATIC)for(Script.Hook h:selected)if(h.kind.equals("field")&&h.memberOwner.equals(owner)&&h.memberName.equals(field)&&h.memberDescriptor.equals(d))dispatch(h,d);}
     @Override public void visitMethodInsn(int op,String owner,String method,String d,boolean iface){
      // Only the visible system-message argument; original logging/narration and packets stay intact.
      for(Script.Hook h:selected)if(h.kind.equals("system_text")&&op==Opcodes.INVOKEVIRTUAL&&owner.equals("net/minecraft/class_338")&&method.equals("method_1812")&&d.equals("(Lnet/minecraft/class_2561;)V"))dispatch(h,Script.TEXT);
      super.visitMethodInsn(op,owner,method,d,iface);for(Script.Hook h:selected)if(h.kind.equals("call")&&h.memberOwner.equals(owner)&&h.memberName.equals(method)&&h.memberDescriptor.equals(d))dispatch(h,Type.getReturnType(d).getDescriptor());}
    };
   }
  },0);
  for(Script.Hook h:hooks){int n=found.getOrDefault(h.id,0);if(n==0||((h.kind.equals("field")||h.kind.equals("call")||h.kind.equals("system_text"))&&n!=h.sites))throw new IllegalArgumentException("Hook target/site mismatch "+h.id+" found="+n);}
  return writer.toByteArray();
 }
}
