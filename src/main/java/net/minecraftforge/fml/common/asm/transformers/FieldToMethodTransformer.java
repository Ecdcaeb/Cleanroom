package net.minecraftforge.fml.common.asm.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;
import java.util.function.Predicate;

public class FieldToMethodTransformer implements IClassTransformer
{
    private static final int[] H_OPCODES = {
            0,                          // invalid
            Opcodes.GETFIELD,           // H_GETFIELD
            Opcodes.GETSTATIC,          // H_GETSTATIC
            Opcodes.PUTFIELD,           // H_PUTFIELD
            Opcodes.PUTSTATIC,          // H_PUTSTATIC
            Opcodes.INVOKEVIRTUAL,      // H_INVOKEVIRTUAL
            Opcodes.INVOKESTATIC,       // H_INVOKESTATIC
            Opcodes.INVOKESPECIAL,      // H_INVOKESPECIAL
            Opcodes.INVOKESPECIAL,      // H_NEWINVOKESPECIAL
            Opcodes.INVOKEINTERFACE     // H_INVOKEINTERFACE
    };
    private final String clsName;
    private final String field;
    private final Handle method;
    private final Predicate<MethodNode> bypass;

    /**
     * @param cls the name of the class to transform
     * @param field the field name
     * @param method the method, belongs to the cls instance
     * @param bypass a predicate to check whether a method should be ignored
     */
    protected FieldToMethodTransformer(String cls, String field, Handle method, Predicate<MethodNode> bypass)
    {
        this.clsName = cls;
        this.field = field;
        this.method = method;
        this.bypass = bypass;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (!this.clsName.equals(transformedName))
            return basicClass;

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);

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
                        it.set(new MethodInsnNode(H_OPCODES[method.getTag()], method.getName(), method.getName(), method.getDesc(), method.isInterface()));
                    }
                }
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

}

