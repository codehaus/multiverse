package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.objectweb.asm.tree.InsnList;

/**
 * @author Peter Veentjer
 */
public interface InstructionTransformer {

    void transform(int index, InsnList original, InsnList output);
}
