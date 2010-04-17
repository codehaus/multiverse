package org.multiverse.stms.alpha.instrumentation.gettersetter;

import org.multiverse.instrumentation.AbstractInstrumentationPhase;
import org.multiverse.instrumentation.Clazz;
import org.multiverse.instrumentation.Environment;
import org.multiverse.instrumentation.asm.AsmUtils;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * A {@link org.multiverse.instrumentation.InstrumentationPhase} responsible for inlining getters
 * and setters on transactional objects.
 * <p/>
 * The inlining only works on transactional methods calling transactional getters/setters.
 *
 * @author Peter Veentjer
 */
public final class GetterSetterInlineInstrumentationPhase extends AbstractInstrumentationPhase {

    public GetterSetterInlineInstrumentationPhase() {
        super("GetterSetterInlineInstrumentationPhase");
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        if (!environment.optimize()) {
            return originalClazz;
        }

        ClassMetadata classMetadata = environment.getMetadataRepository().loadClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!classMetadata.hasTransactionalMethods()) {
            return originalClazz;
        }

        ClassNode originalClassNode = AsmUtils.loadAsClassNode(originalClazz.getBytecode());
        GetterSetterInlineTransformer transformer = new GetterSetterInlineTransformer(
                originalClassNode,
                classMetadata,
                environment.getMetadataRepository(),
                originalClazz.getClassLoader(),
                environment.getLog());

        ClassNode transformed = transformer.transform();
        byte[] newBytecode = toBytecode(transformed);
        return new Clazz(originalClazz, newBytecode);
    }
}
