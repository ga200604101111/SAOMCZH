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
  ClassWriter writer=new ClassWriter(input,ClassWriter.COMPUTE_MAXS);Map<String,Integer>found=new HashMap<>();
  input.accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[]exceptions){
    MethodVisitor original=super.visitMethod(access,name,desc,sig,exceptions);
    List<Script.Hook>selected=new ArrayList<>();for(Script.Hook h:hooks)if(h.method.equals(name)&&h.descriptor.equals(desc))selected.add(h);
    if(selected.isEmpty())return original;if((access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT))!=0)throw new IllegalArgumentException("Method has no transformable body");
    return new MethodVisitor(Opcodes.ASM9,original){
     void dispatch(Script.Hook h,String valueDescriptor){
      super.visitLdcInsn(h.id);super.visitInsn(Opcodes.SWAP);super.visitLdcInsn(Type.getObjectType(cls));
      boolean string=valueDescriptor.equals(Script.STR);
      super.visitMethodInsn(Opcodes.INVOKESTATIC,"sao/zh/api/Dispatch",string?"text":"value",string?"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/String;":"(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;",false);
      if(!string)super.visitTypeInsn(Opcodes.CHECKCAST,Type.getType(valueDescriptor).getInternalName());found.merge(h.id,1,Integer::sum);
     }
     @Override public void visitCode(){super.visitCode();for(Script.Hook h:selected)if(h.kind.equals("argument")){
      Type[]args=Type.getArgumentTypes(desc);int slot=(access&Opcodes.ACC_STATIC)==0?1:0;for(int i=0;i<h.argument;i++)slot+=args[i].getSize();
      super.visitVarInsn(Opcodes.ALOAD,slot);dispatch(h,args[h.argument].getDescriptor());super.visitVarInsn(Opcodes.ASTORE,slot);
     }}
     @Override public void visitInsn(int op){if(op==Opcodes.ARETURN)for(Script.Hook h:selected)if(h.kind.equals("return"))dispatch(h,Type.getReturnType(desc).getDescriptor());super.visitInsn(op);}
     @Override public void visitFieldInsn(int op,String owner,String field,String d){super.visitFieldInsn(op,owner,field,d);if(op==Opcodes.GETFIELD||op==Opcodes.GETSTATIC)for(Script.Hook h:selected)if(h.kind.equals("field")&&h.memberOwner.equals(owner)&&h.memberName.equals(field)&&h.memberDescriptor.equals(d))dispatch(h,d);}
     @Override public void visitMethodInsn(int op,String owner,String method,String d,boolean iface){super.visitMethodInsn(op,owner,method,d,iface);for(Script.Hook h:selected)if(h.kind.equals("call")&&h.memberOwner.equals(owner)&&h.memberName.equals(method)&&h.memberDescriptor.equals(d))dispatch(h,Type.getReturnType(d).getDescriptor());}
    };
   }
  },0);
  for(Script.Hook h:hooks){int n=found.getOrDefault(h.id,0);if(n==0||((h.kind.equals("field")||h.kind.equals("call"))&&n!=h.sites))throw new IllegalArgumentException("Hook target/site mismatch "+h.id+" found="+n);}
  return writer.toByteArray();
 }
}
