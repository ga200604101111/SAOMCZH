package sao.zh.compat16;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Alters only the 1.21.4 ResourcePackManager discovery and selection return paths. */
public final class RepositoryTransformer implements ClassFileTransformer {
    static final String TARGET = "net/minecraft/class_3283";
    static final String OWNER = "sao/zh/compat16/PackBridge";
    static final String DISCOVER = "method_29212";
    static final String SELECT = "method_29208";

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> redef, ProtectionDomain domain, byte[] original) {
        if (!TARGET.equals(name) || original == null) return null;
        try {
            byte[] changed = patch(original);
            DirectAgent.log("RESOURCE_MANAGER_TRANSFORMED " + name);
            return changed;
        } catch (Throwable e) {
            DirectAgent.log("TRANSFORM_FAILED " + e.getClass().getSimpleName());
            return null;
        }
    }

    static byte[] patch(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] counts = new int[2];
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String method, String desc, String sign, String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, method, desc, sign, exceptions);
                if (parent == null) return null;
                int mode = 0;
                if (DISCOVER.equals(method) && "()Ljava/util/Map;".equals(desc)) mode = 1;
                if (SELECT.equals(method) && "(Ljava/util/Collection;)Ljava/util/List;".equals(desc)) mode = 2;
                if (mode == 0) return parent;
                final int selected = mode;
                counts[selected - 1]++;
                return new MethodVisitor(Opcodes.ASM9, parent) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN) {
                            // Stack before: Map or List, followed by manager object for reflection.
                            super.visitLdcInsn(org.objectweb.asm.Type.getObjectType(TARGET));
                            if (selected == 1) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER,
                                    "addPack", "(Ljava/util/Map;Ljava/lang/Class;)Ljava/util/Map;", false);
                            } else {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER,
                                    "forceActive", "(Ljava/util/List;Ljava/lang/Class;)Ljava/util/List;", false);
                            }
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        };
        reader.accept(cv, 0);
        if (counts[0] != 1 || counts[1] != 1) {
            throw new IllegalStateException("Unsupported ResourcePackManager signature");
        }
        return writer.toByteArray();
    }

    public static int verify(byte[] input) {
        byte[] changed = patch(input);
        final int[] discovered = new int[2];
        new ClassReader(changed).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String method, String desc, String sign, String[] exceptions) {
                if (DISCOVER.equals(method) && "()Ljava/util/Map;".equals(desc)) {
                    return checking(0);
                }
                if (SELECT.equals(method) && "(Ljava/util/Collection;)Ljava/util/List;".equals(desc)) {
                    return checking(1);
                }
                return null;
            }
            private MethodVisitor checking(final int index) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && OWNER.equals(owner)) {
                            if (index == 0 && "addPack".equals(name)) discovered[index]++;
                            if (index == 1 && "forceActive".equals(name)) discovered[index]++;
                        }
                    }
                };
            }
        }, 0);
        if (discovered[0] != 1 || discovered[1] != 1) {
            throw new IllegalStateException("Injection count mismatch " + discovered[0] + "/" + discovered[1]);
        }
        return discovered[0] + discovered[1];
    }
}
