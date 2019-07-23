package net.alterorb.deobfuscator.transformer.impl;

import jdk.internal.org.objectweb.asm.Opcodes;
import lombok.extern.log4j.Log4j2;
import net.alterorb.deobfuscator.DeobfuscationContext;
import net.alterorb.deobfuscator.transformer.Transformer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

@Log4j2
public class IfJumpTransformer implements Transformer {

    @Override
    public void process(DeobfuscationContext ctx) {

        ctx.getClassNodes().forEach(classNode -> {

            for (MethodNode method : classNode.methods) {
                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

                while (iterator.hasNext()) {
                    AbstractInsnNode insnNode = iterator.next();

                    if (insnNode.getOpcode() >= Opcodes.IFEQ && insnNode.getOpcode() <= Opcodes.IF_ACMPNE) {
                        JumpInsnNode ifInsnNode = (JumpInsnNode) insnNode;
                        AbstractInsnNode targetInsnNode = ifInsnNode.label.getNext();

                        if (targetInsnNode.getOpcode() == Opcodes.GOTO) {
                            JumpInsnNode gotoInsnNode = (JumpInsnNode) targetInsnNode;

                            ifInsnNode.label = gotoInsnNode.label;
                        }
                    }
                }
            }
        });
    }
}
