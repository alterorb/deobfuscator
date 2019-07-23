package net.alterorb.deobfuscator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;

public class DeobUtils {

    public static int extractIntValue(AbstractInsnNode abstractInsnNode) {
        switch (abstractInsnNode.getType()) {

            case AbstractInsnNode.INSN:
                return abstractInsnNode.getOpcode() - Opcodes.ICONST_M1 - 1;

            case AbstractInsnNode.INT_INSN:
                IntInsnNode keyVal = (IntInsnNode) abstractInsnNode;
                return keyVal.operand;

            default:
                throw new IllegalStateException("Unhandled insn type " + abstractInsnNode.getClass());
        }
    }
}
