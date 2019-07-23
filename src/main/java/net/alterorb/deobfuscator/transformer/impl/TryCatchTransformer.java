package net.alterorb.deobfuscator.transformer.impl;

import net.alterorb.deobfuscator.DeobfuscationContext;
import net.alterorb.deobfuscator.transformer.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Objects;

public class TryCatchTransformer implements Transformer {

    @Override
    public void process(DeobfuscationContext ctx) {
        ctx.getClassNodes().forEach(classNode -> {
            List<MethodNode> methods = classNode.methods;

            for (MethodNode method : methods) {
                method.tryCatchBlocks.removeIf(tryCatch -> Objects.equals(tryCatch.type, "java/lang/RuntimeException"));
            }
        });
    }
}
