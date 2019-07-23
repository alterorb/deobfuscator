package net.alterorb.deobfuscator.transformer;

import net.alterorb.deobfuscator.DeobfuscationContext;

public interface Transformer {

    void process(DeobfuscationContext ctx);

}
