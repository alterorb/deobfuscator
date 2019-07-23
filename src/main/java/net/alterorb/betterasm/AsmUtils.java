package net.alterorb.betterasm;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AsmUtils {

    public static List<ClassNode> loadJarClasses(Path pathToJar) throws IOException {
        return loadJarClasses(pathToJar, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    public static List<ClassNode> loadJarClasses(Path pathToJar, int parsingOptions) throws IOException {
        ArrayList<ClassNode> classNodes = new ArrayList<>();

        try (JarFile jarFile = new JarFile(pathToJar.toString())) {
            Enumeration<?> enums = jarFile.entries();

            while (enums.hasMoreElements()) {
                JarEntry entry = (JarEntry) enums.nextElement();

                if (entry.getName().endsWith(".class")) {
                    ClassReader classReader = new ClassReader(jarFile.getInputStream(entry));
                    ClassNode classNode = new ClassNode();

                    classReader.accept(classNode, parsingOptions);
                    classNodes.add(classNode);
                }
            }
        }
        return classNodes;
    }
}
