package net.alterorb.deobfuscator.transformer.impl;

import net.alterorb.deobfuscator.DeobUtils;
import net.alterorb.deobfuscator.DeobfuscationContext;
import net.alterorb.deobfuscator.transformer.Transformer;
import lombok.extern.log4j.Log4j2;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * Decrypts all strings and removes the decryption methods
 */
@Log4j2
public class StringDecryptorTransformer implements Transformer {

    private static final Type[] STRING_DECRYPTOR_ARGUMENTS = {Type.getType(String.class)};
    private static final Type[] CHAR_ARRAY_DECRYPTOR_ARGUMENTS = {Type.getType(char[].class)};

    @Override
    public void process(DeobfuscationContext ctx) {

        ctx.getClassNodes().forEach(classNode -> {
            MethodNode stringDecryptorMethod = classNode.findFirstMethodByDescriptor(Type.getType(char[].class), STRING_DECRYPTOR_ARGUMENTS);
            MethodNode charArrayDecryptorMethod = classNode.findFirstMethodByDescriptor(Type.getType(String.class), CHAR_ARRAY_DECRYPTOR_ARGUMENTS);

            if (stringDecryptorMethod == null) {
                LOGGER.debug("Skipping class '{}' because we couldn't find the decryptor methods!", classNode.name);
                return;
            }

            LOGGER.debug("Found string encryptors in the class '{}'", classNode.name);
            int stringKey = 0;
            ListIterator<AbstractInsnNode> iterator = stringDecryptorMethod.instructions.iterator();

            while (iterator.hasNext()) {
                AbstractInsnNode next = iterator.next();

                if (next.getOpcode() == Opcodes.CALOAD) {
                    stringKey = DeobUtils.extractIntValue(next.getNext());
                    break;
                }
            }
            LOGGER.debug("String decryption key={}", stringKey);

            TableSwitchInsnNode tableSwitchNode = findTableSwitchNode(charArrayDecryptorMethod);
            byte[] keys = new byte[5];

            if (tableSwitchNode != null) {
                List<LabelNode> labels = tableSwitchNode.labels;

                for (int i = 0; i < labels.size(); i++) {
                    AbstractInsnNode abstractInsnNode = labels.get(i).getNext();

                    keys[i] = (byte) DeobUtils.extractIntValue(abstractInsnNode);
                }
                keys[4] = (byte) DeobUtils.extractIntValue(tableSwitchNode.dflt.getNext());
                LOGGER.debug("Char array decryption keys={}", Arrays.toString(keys));
            }

            List<MethodNode> methods = classNode.methods;

            for (MethodNode method : methods) {
                AbstractInsnNode[] abstractInsnNodes = method.instructions.toArray(); // we make a 'copy' because we'll be removing elements from the InsnList

                for (AbstractInsnNode abstractInsnNode : abstractInsnNodes) {

                    if (abstractInsnNode.getType() == AbstractInsnNode.LDC_INSN) {
                        LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
                        AbstractInsnNode ldcInsnNodeNext = ldcInsnNode.getNext();

                        if (!(ldcInsnNode.cst instanceof String) || ldcInsnNodeNext == null || ldcInsnNodeNext.getType() != AbstractInsnNode.METHOD_INSN
                                || !isStringDecryptorMethod((MethodInsnNode) ldcInsnNodeNext)) {
                            continue;
                        }

                        String deobfuscatedString = deobfuscate(deobfuscate((String) ldcInsnNode.cst, stringKey), keys);
                        LOGGER.debug("Decrypted string '{}' into '{}'", ldcInsnNode.cst, deobfuscatedString);

                        // replaces the cst with the deobfuscated string and removes the call to the string ecryptors
                        ldcInsnNode.cst = deobfuscatedString;
                        AbstractInsnNode decryptorNextInsnNode = ldcInsnNodeNext.getNext();
                        method.instructions.remove(ldcInsnNodeNext); // z(String) char[] call
                        method.instructions.remove(decryptorNextInsnNode); // z(char[]) String call
                    }
                }
            }
            methods.remove(stringDecryptorMethod);
            methods.remove(charArrayDecryptorMethod);
        });
    }

    private static boolean isStringDecryptorMethod(MethodInsnNode methodInsnNode) {
        return Objects.equals(methodInsnNode.name, "z")
                && Objects.equals(Type.getReturnType(methodInsnNode.desc), Type.getType(char[].class))
                && Arrays.equals(Type.getArgumentTypes(methodInsnNode.desc), STRING_DECRYPTOR_ARGUMENTS);
    }

    private static TableSwitchInsnNode findTableSwitchNode(MethodNode methodNode) {
        ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();

        while (iterator.hasNext()) {
            AbstractInsnNode next = iterator.next();

            if (next.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
                return (TableSwitchInsnNode) next;
            }
        }
        return null;
    }

    private static char[] deobfuscate(String string, int key) {
        char[] chars = string.toCharArray();

        if (chars.length < 2) {
            chars[0] = (char) (chars[0] ^ key);
        }
        return chars;
    }

    private static String deobfuscate(char[] obfuscatedChars, byte[] keys) {
        int length = obfuscatedChars.length;

        for (int i = 0; length > i; ++i) {
            char aChar = obfuscatedChars[i];
            byte key;
            switch (i % 5) {
                case 0:
                    key = keys[0];
                    break;
                case 1:
                    key = keys[1];
                    break;
                case 2:
                    key = keys[2];
                    break;
                case 3:
                    key = keys[3];
                    break;
                default:
                    key = keys[4];
            }
            obfuscatedChars[i] = (char) (aChar ^ key);
        }
        return new String(obfuscatedChars);
    }
}
