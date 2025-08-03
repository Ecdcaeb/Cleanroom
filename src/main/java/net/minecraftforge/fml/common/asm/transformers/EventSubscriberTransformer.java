/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.common.asm.transformers;

import java.lang.reflect.Modifier;
import java.util.*;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class EventSubscriberTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass){
        if (basicClass == null) return null;

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        LinkedList<MethodNode> subscribers = new LinkedList<>();

        for (MethodNode methodNode : classNode.methods)
        {
            List<AnnotationNode> anns = methodNode.visibleAnnotations;

            if (anns != null && anns.stream().anyMatch((v)-> "Lnet/minecraftforge/fml/common/eventhandler/SubscribeEvent;".equals(v.desc)))
            {
                subscribers.add(methodNode);
            }
        }

        if (!subscribers.isEmpty())
        {
            for (MethodNode sub : subscribers) {
                MethodNode methodNode = new MethodNode(
                        toPublic(sub.access),
                        "_cleanroom_eventbus_" + sub.name + "_"+sub.desc.hashCode(),
                        "()Lnet/minecraftforge/fml/common/eventhandler/IEventListener;",
                        null, null);
                if (Modifier.isStatic(methodNode.access)) {
                    methodNode.visitInvokeDynamicInsn(
                            "invoke",
                            "()Lnet/minecraftforge/fml/common/eventhandler/IEventListener;",
                            new Handle(Opcodes.H_INVOKESTATIC,
                                    "java/lang/invoke/LambdaMetafactory",
                                    "metafactory",
                                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                                    false),
                            new Object[]{
                                    Type.getType("(Lnet/minecraftforge/fml/common/eventhandler/Event;)V"),
                                    new Handle(Opcodes.H_INVOKESTATIC,
                                            classNode.name,
                                            sub.name,
                                            sub.desc,
                                            Modifier.isInterface(classNode.access)),
                                    Type.getType(sub.desc.substring(0, sub.desc.lastIndexOf(')')) + ")V")
                            }
                    );
                    methodNode.visitInsn(Opcodes.ARETURN);
                } else {
                    methodNode.visitVarInsn(Opcodes.ALOAD, 0);
                    methodNode.visitInvokeDynamicInsn(
                            "invoke",
                            "(L"+ classNode.name +" ;)Lnet/minecraftforge/fml/common/eventhandler/IEventListener;",
                            new Handle(Opcodes.H_INVOKESTATIC,
                                    "java/lang/invoke/LambdaMetafactory",
                                    "metafactory",
                                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                                    false),
                            new Object[]{
                                    Type.getType("(Lnet/minecraftforge/fml/common/eventhandler/Event;)V"),
                                    new Handle(Modifier.isInterface(classNode.access) ? Opcodes.H_INVOKEINTERFACE : Opcodes.H_INVOKEVIRTUAL,
                                            classNode.name,
                                            sub.name,
                                            sub.desc,
                                            Modifier.isInterface(classNode.access)),
                                    Type.getType(sub.desc.substring(0, sub.desc.lastIndexOf(')')) + ")V")
                            }
                    );
                    methodNode.visitInsn(Opcodes.ARETURN);
                }
                classNode.methods.add(methodNode);
            }
            classNode.access = toPublic(classNode.access);

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();
        }

        return basicClass;
    }

    private static int toPublic(int access) {
        return access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
    }
}
