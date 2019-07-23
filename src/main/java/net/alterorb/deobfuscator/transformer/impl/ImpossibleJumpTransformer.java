package net.alterorb.deobfuscator.transformer.impl;

import lombok.extern.log4j.Log4j2;
import net.alterorb.deobfuscator.DeobfuscationContext;
import net.alterorb.deobfuscator.transformer.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Objects;

/**
 * Attempts to remove impossible conditions that reference main class constants
 * getstatic OrbDefence.D:boolean
 * istore7 // dummy is stored on local var 7
 * <p>
 * iload2
 * iload7 // injected load
 * ifne L11 // injected condition
 * if_icmpeq L12
 * <p>
 * This obfuscation seems to be limited to zero comparisons only (ifne, ifeq, ifge, ifgt, ifle, iflt)
 */
@Log4j2
public class ImpossibleJumpTransformer implements Transformer {

    @Override
    public void process(DeobfuscationContext ctx) {

        ctx.getClassNodes().forEach(classNode -> {

            for (MethodNode method : classNode.methods) {
                AbstractInsnNode first = method.instructions.getFirst();

                if (first == null) {
                    continue;
                }

                if (first.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) first;

                    if (Objects.equals(fieldInsnNode.owner, ctx.getTargetJarMainClass())) {
                        AbstractInsnNode next = first.getNext();

                        if (next.getOpcode() == Opcodes.ISTORE) {
                            VarInsnNode varInsnNode = (VarInsnNode) next;
                            LOGGER.warn("Suspicious load to local variable: {}.{}({}), local idx={} at method {}.{}({})", fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc, varInsnNode.var, classNode.name,
                                    method.name, method.desc);
                            cleanMethod(method, varInsnNode.var);
                            continue;
                        }
                    }
                }
            }
        });
    }

    private void cleanMethod(MethodNode methodNode, int localVarIndex) {
        // ifne -> next
        // ifeq -> follow jump
        InsnList instructions = methodNode.instructions;
        AbstractInsnNode[] abstractInsnNodes = instructions.toArray();

        for (AbstractInsnNode abstractInsnNode : abstractInsnNodes) {

            if (abstractInsnNode.getOpcode() == Opcodes.ISTORE) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;

                if (varInsnNode.var == localVarIndex) {
                    instructions.remove(varInsnNode.getPrevious());
                    instructions.remove(varInsnNode);
                }
            }

            if (abstractInsnNode.getOpcode() == Opcodes.ILOAD) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;

                if (varInsnNode.var == localVarIndex) {
                    instructions.remove(varInsnNode.getNext());
                    instructions.remove(varInsnNode);
                }
            }
        }
    }
}
