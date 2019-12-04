package net.alterorb.deobfuscator.transformer.impl;

import net.alterorb.betterasm.ClassNode;
import net.alterorb.deobfuscator.DeobfuscationContext;
import net.alterorb.deobfuscator.transformer.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

/**
 * Simplifies bitshifts by overly large constants, ie: var0 >> 1171161633 -> var0 >> 1
 */
public class SimplifyBitshiftTransformer implements Transformer {

    private static final int MASK = 0x1F;

    @Override
    public void process(DeobfuscationContext ctx) {

        for (ClassNode classNode : ctx.getClassNodes()) {

            for (MethodNode method : classNode.methods) {
                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

                while (iterator.hasNext()) {
                    AbstractInsnNode insnNode = iterator.next();

                    if (insnNode.getOpcode() == Opcodes.ISHR || insnNode.getOpcode() == Opcodes.ISHL) {
                        AbstractInsnNode previous = insnNode.getPrevious();

                        if (previous.getType() == AbstractInsnNode.LDC_INSN) {
                            LdcInsnNode ldcInsnNode = (LdcInsnNode) previous;
                            Object constant = ldcInsnNode.cst;

                            if (constant instanceof Integer) {
                                ldcInsnNode.cst = (Integer) constant & MASK;
                            }
                        }
                    }
                }
            }
        }
    }
}
