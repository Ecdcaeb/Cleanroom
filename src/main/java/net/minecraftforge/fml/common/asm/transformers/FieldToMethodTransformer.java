package net.minecraftforge.fml.common.asm.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;
import java.util.function.Predicate;

public class FieldToMethodTransformer implements IClassTransformer {
    private final String clsName;
    private final String field;
    private final String method;
    private final Predicate<MethodNode> bypass;
    private final boolean isInterface;

    /**
     * @param cls the name of the class to transform
     * @param field the field name
     * @param method the method, belongs to the cls instance
     * @param isInterface is the method is a interface method
     * @param bypass a predicate to check whether a method should be ignored
     */
    protected FieldToMethodTransformer(String cls, String field, String method, boolean isInterface, Predicate<MethodNode> bypass) {
        this.clsName = cls;
        this.field = field;
        this.method = method;
        this.bypass = bypass;
        this.isInterface = isInterface;
    }

    protected FieldToMethodTransformer(String cls, String field, String method, boolean isInterface) {
        this(cls, field, method, isInterface, mn -> method.equals(mn.name));
    }

    protected FieldToMethodTransformer(String cls, String srgField, String method) {
        this(cls, FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(cls.replace('.', '/'), srgField, null), method, false);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!this.clsName.equals(transformedName))
            return basicClass;

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);

        //check field
        FieldNode fieldRef = null;
        for(FieldNode fn : classNode.fields){
            if (this.field.equals(fn.name)){
                fieldRef = fn;
                break;
            }
        }
        if (fieldRef == null) throw new RuntimeException("Error processing " + clsName + " - no holder field " + field + " declared.");


        for (MethodNode m: classNode.methods)
        {
            if (this.bypass.test(m)) continue;
            for (ListIterator<AbstractInsnNode> it = m.instructions.iterator(); it.hasNext(); )
            {
                AbstractInsnNode insNode = it.next();
                if (insNode.getType() == AbstractInsnNode.FIELD_INSN)
                {
                    FieldInsnNode fi = (FieldInsnNode)insNode;
                    if (field.equals(fi.name) && fi.getOpcode() == Opcodes.GETFIELD)
                    {
                        it.set(new MethodInsnNode(this.isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, classNode.name, method, "()" + fieldRef.desc, this.isInterface));
                    }
                }
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

}

