package org.multiverse.stms.alpha.instrumentation.tranlocal;

import org.multiverse.instrumentation.AbstractInstrumentationPhase;
import org.multiverse.instrumentation.Clazz;
import org.multiverse.instrumentation.Environment;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.asm.AsmUtils.loadAsClassNode;
import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * @author Peter Veentjer
 */
public final class TranlocalInstrumentationPhase extends AbstractInstrumentationPhase {

    public TranlocalInstrumentationPhase() {
        super("TranlocalInstrumentationPhase");
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        ClassLoader classLoader = originalClazz.getClassLoader();

        ClassMetadata classMetadata = environment.getMetadataRepository().loadClassMetadata(
                classLoader, originalClazz.getName());

        if (!classMetadata.isTransactionalObjectWithObjectGranularFields()) {
            return originalClazz;
        }

        ClassNode originalClassNode = loadAsClassNode(originalClazz.getBytecode());

        TranlocalFactory transformer = new TranlocalFactory(
                classLoader, originalClassNode, environment.getMetadataRepository());
        ClassNode result = transformer.create();

        Clazz tranlocalClazz = new Clazz(classMetadata.getTranlocalName());
        tranlocalClazz.setBytecode(toBytecode(result));
        tranlocalClazz.setClassLoader(originalClazz.getClassLoader());

        environment.getFiler().createClassFile(tranlocalClazz);

        return originalClazz;
    }
}
