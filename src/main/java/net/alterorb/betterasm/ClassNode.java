package net.alterorb.betterasm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClassNode extends org.objectweb.asm.tree.ClassNode {

    private MethodNode noArgsConstructor;
    private MethodNode classInitializer;

    public ClassNode() {
        this(Opcodes.ASM6);
    }

    public ClassNode(int api) {
        super(api);
    }

    public MethodNode getNoArgsConstructor() {

        if (noArgsConstructor == null) {
            noArgsConstructor = findFirstMethodMatching(methodNode -> Objects.equals(methodNode.name, "<init>") && Type.getArgumentTypes(methodNode.desc).length == 0);
        }
        return noArgsConstructor;
    }

    public MethodNode getClassInitializer() {

        if (classInitializer == null) {
            classInitializer = findFirstMethodMatching(methodNode -> Objects.equals(methodNode.name, "<clinit>"));
        }
        return classInitializer;
    }

    public List<FieldNode> findFieldsByType(Type type) {
        return findFieldsMatching(fieldNode -> Objects.equals(Type.getType(fieldNode.desc), type));
    }

    public List<FieldNode> findFieldsMatching(Predicate<FieldNode> predicate) {
        return fields.stream().filter(predicate).collect(Collectors.toList());
    }

    public FieldNode findFirstFieldMatching(Predicate<FieldNode> predicate) {
        return fields.stream().filter(predicate).findFirst().orElse(null);
    }

    public List<MethodNode> findMethodsByDescriptor(Type returnType, Type... argTypes) {
        return findMethodsMatching(methodNode -> Objects.equals(Type.getReturnType(methodNode.desc), returnType) && Arrays.equals(Type.getArgumentTypes(methodNode.desc), argTypes));
    }

    public MethodNode findFirstMethodByDescriptor(Type returnType, Type... argTypes) {
        return findFirstMethodMatching(methodNode -> Objects.equals(Type.getReturnType(methodNode.desc), returnType) && Arrays.equals(Type.getArgumentTypes(methodNode.desc), argTypes));
    }

    public List<MethodNode> findMethodsMatching(Predicate<MethodNode> predicate) {
        return methods.stream().filter(predicate).collect(Collectors.toList());
    }

    public MethodNode findFirstMethodMatching(Predicate<MethodNode> predicate) {
        return methods.stream().filter(predicate).findFirst().orElse(null);
    }
}
