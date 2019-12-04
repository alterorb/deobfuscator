package net.alterorb.deobfuscator.transformer.impl;

import net.alterorb.deobfuscator.DeobUtils;
import net.alterorb.deobfuscator.DeobfuscationContext;
import net.alterorb.deobfuscator.transformer.Transformer;
import lombok.extern.log4j.Log4j2;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Inlines all strings contained in the static [Ljava/lang/String; array
 */
@Log4j2
public class StringInlinerTransformer implements Transformer {

    @Override
    public void process(DeobfuscationContext ctx) {

        ctx.getClassNodes().forEach(classNode -> {
            FieldNode stringArrayField = classNode.findFirstFieldMatching(fieldNode -> fieldNode.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL) && fieldNode.desc.equals("[Ljava/lang/String;"));

            if (stringArrayField == null) {
                LOGGER.debug("Skipping class '{}' because we couldn't find the zStringArray field!", classNode.name);
                return;
            }

            Map<Integer, Object> cstMap = new HashMap<>();

            // Find our anchor point where we'll step back, from PUTSTATIC a.z[Ljava/lang/String; to anewarray java/lang/String
            MethodNode classInitializer = classNode.getClassInitializer();
            InsnList clinitInstructions = classInitializer.instructions;
            AbstractInsnNode anchorInsnNode = null;

            for (AbstractInsnNode abstractInsnNode : clinitInstructions.toArray()) {

                if (abstractInsnNode.getOpcode() == Opcodes.PUTSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;

                    if (Objects.equals(fieldInsnNode.name, stringArrayField.name) && Objects.equals(fieldInsnNode.desc, "[Ljava/lang/String;")) {
                        anchorInsnNode = fieldInsnNode;
                        break;
                    }
                }
            }

            if (anchorInsnNode == null) {
                LOGGER.warn("Failed to find anchor point!");
                return;
            }

            ArrayList<AbstractInsnNode> forRemoval = new ArrayList<>();
            AbstractInsnNode current = anchorInsnNode;

            forRemoval.add(anchorInsnNode);

            while ((current = current.getPrevious()) != null) {
                int opcode = current.getOpcode();

                forRemoval.add(current);

                if (opcode == Opcodes.ANEWARRAY) {
                    forRemoval.add(current.getPrevious());
                    break;
                } else if (opcode == Opcodes.LDC) {
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) current;
                    int index = DeobUtils.extractIntValue(ldcInsnNode.getPrevious());

                    cstMap.put(index, ldcInsnNode.cst);
                }
            }

            // Couldn't think of a cleaner way right now
            for (AbstractInsnNode abstractInsnNode : forRemoval) {
                clinitInstructions.remove(abstractInsnNode);
            }

            classNode.fields.remove(stringArrayField);

            // replace array loads with inlined values
            /*
             * getstatic a.z [Ljava/lang/String;
             * bipush index
             * aaload
             */
            for (MethodNode method : classNode.methods) {
                InsnList methodInstructions = method.instructions;
                AbstractInsnNode[] abstractInsnNodes = methodInstructions.toArray(); // we make a 'copy' because we'll be modifying the InsnList

                for (AbstractInsnNode abstractInsnNode : abstractInsnNodes) {

                    if (abstractInsnNode.getOpcode() == Opcodes.GETSTATIC) {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;

                        if (Objects.equals(fieldInsnNode.name, stringArrayField.name) && Objects.equals(fieldInsnNode.desc, "[Ljava/lang/String;")) {
                            int loadIndex = DeobUtils.extractIntValue(fieldInsnNode.getNext());

                            methodInstructions.remove(fieldInsnNode.getNext().getNext()); // aaload
                            methodInstructions.remove(fieldInsnNode.getNext()); // index

                            Object cst = cstMap.get(loadIndex);

                            if (cst == null) {
                                LOGGER.warn("No cst for load index {}!", loadIndex);
                                continue;
                            }
                            methodInstructions.set(fieldInsnNode, new LdcInsnNode(cst));
                        }
                    }
                }
            }
        });
    }
}
