package org.multiverse.stms.alpha.instrumentation.fieldgranularity;

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
public final class FieldGranularityInstrumentationPhase extends AbstractInstrumentationPhase {

    public FieldGranularityInstrumentationPhase() {
        super("FieldGranularityInstrumentationPhase");
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        ClassMetadata metadata = environment.getMetadataRepository().loadClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!metadata.hasManagedFieldsWithFieldGranularity()) {
            return originalClazz;
        }

        ClassNode originalNode = loadAsClassNode(originalClazz.getBytecode());

        FieldGranularityTransformer transformer = new FieldGranularityTransformer(
                originalClazz.getClassLoader(),
                originalNode,
                environment.getMetadataRepository());

        ClassNode transformed = transformer.transform();

        return new Clazz(originalClazz, toBytecode(transformed));
    }
}
