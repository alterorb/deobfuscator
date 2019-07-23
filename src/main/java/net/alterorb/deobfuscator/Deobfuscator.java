package net.alterorb.deobfuscator;

import lombok.extern.log4j.Log4j2;
import net.alterorb.betterasm.AsmUtils;
import net.alterorb.betterasm.ClassNode;
import net.alterorb.deobfuscator.transformer.Transformer;
import net.alterorb.deobfuscator.transformer.impl.IfJumpTransformer;
import net.alterorb.deobfuscator.transformer.impl.ImpossibleJumpTransformer;
import net.alterorb.deobfuscator.transformer.impl.SimplifyBitshiftTransformer;
import net.alterorb.deobfuscator.transformer.impl.StringDecryptorTransformer;
import net.alterorb.deobfuscator.transformer.impl.StringInlinerTransformer;
import net.alterorb.deobfuscator.transformer.impl.TryCatchTransformer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@Log4j2
public class Deobfuscator {

    private static final List<Transformer> TRANSFORMERS = new ArrayList<>();

    static {
        TRANSFORMERS.add(new StringDecryptorTransformer());
        TRANSFORMERS.add(new StringInlinerTransformer());
        TRANSFORMERS.add(new SimplifyBitshiftTransformer());
        TRANSFORMERS.add(new TryCatchTransformer());
        TRANSFORMERS.add(new IfJumpTransformer());
        TRANSFORMERS.add(new ImpossibleJumpTransformer());
    }

    public static void main(String[] args) throws IOException {

        if (args.length <= 0) {
            LOGGER.info("Missing required parameter: Path to obfuscated jar");
            return;
        }
        Path jarPath = Paths.get(args[0]);
        Path deobJarPath;

        if (args.length >= 2) {
            deobJarPath = Paths.get(args[1]);
        } else {
            String jarName = jarPath.getFileName().toString();
            jarName = jarName.substring(0, jarName.lastIndexOf('.'));
            deobJarPath = jarPath.resolveSibling(jarName + "-deob.jar");
        }

        LOGGER.info("Obfuscated jar location={}", jarPath);
        List<ClassNode> classNodes = AsmUtils.loadJarClasses(jarPath);
        String mainClassName = findMainClass(classNodes);

        LOGGER.info("Determined that the class {} is the main class name", mainClassName);

        DeobfuscationContext deobfuscationContext = new DeobfuscationContext();
        deobfuscationContext.setTargetJarMainClass(mainClassName);
        deobfuscationContext.setClassNodes(classNodes);

        LOGGER.info("Loaded {} classes, starting to run the transformers...", classNodes.size());

        for (Transformer transformer : TRANSFORMERS) {
            LOGGER.info("Transforming classes with {}", transformer.getClass().getSimpleName());
            transformer.process(deobfuscationContext);
        }
        LOGGER.info("Writing classes to {}", deobJarPath);

        try (JarOutputStream output = new JarOutputStream(new FileOutputStream(deobJarPath.toFile()))) {

            for (ClassNode node : classNodes) {
                JarEntry entry = new JarEntry(node.name + ".class");
                output.putNextEntry(entry);

                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                node.accept(writer);
                output.write(writer.toByteArray());

                output.closeEntry();
            }
        }
    }

    private static String findMainClass(List<ClassNode> classNodes) {

        for (ClassNode classNode : classNodes) {

            if (classNode.name.length() > 3) {
                return classNode.name;
            }
        }
        throw new RuntimeException("Could not determine the main class");
    }
}
