package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.instrumentation.compiler.AbstractCompilePhase;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Environment;
import org.multiverse.stms.alpha.instrumentation.asm.NonTransactionalMethodFieldAccessTransformer;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.asm.AsmUtils.loadAsClassNode;
import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * @author Peter Veentjer
 */
public class NonTransactionalObjectFieldAccessCompilePhase
        extends AbstractCompilePhase {

    public NonTransactionalObjectFieldAccessCompilePhase() {
        super("NonTransactionalObjectFieldAccessCompilePhase");
    }

    @Override
    protected Clazz doCompile(Environment environment, Clazz originalClazz) {
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
