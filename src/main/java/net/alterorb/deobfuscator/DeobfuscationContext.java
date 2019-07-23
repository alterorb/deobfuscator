package net.alterorb.deobfuscator;

import lombok.Data;
import net.alterorb.betterasm.ClassNode;

import java.util.List;

@Data
public class DeobfuscationContext {

    private List<ClassNode> classNodes;
    private String targetJarMainClass;
}
