package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.multiverse.instrumentation.AbstractInstrumentationPhase;
import org.multiverse.instrumentation.Clazz;
import org.multiverse.instrumentation.Environment;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.asm.AsmUtils.loadAsClassNode;
import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * @author Peter Veentjer
 */
public final class NonTransactionalObjectFieldAccessInstrumentationPhase
        extends AbstractInstrumentationPhase {

    public NonTransactionalObjectFieldAccessInstrumentationPhase() {
        super("NonTransactionalObjectFieldAccessInstrumentationPhase");
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        ClassNode original = loadAsClassNode(originalClazz.getBytecode());

        NonTransactionalMethodFieldAccessTransformer transformer = new NonTransactionalMethodFieldAccessTransformer(
                originalClazz.getClassLoader(), original, environment.getMetadataRepository());

        ClassNode transformed = transformer.transform();

        if (transformed == null) {
            return originalClazz;
        }

        byte[] newBytecode = toBytecode(transformed);
        return new Clazz(originalClazz, newBytecode);
    }
}
