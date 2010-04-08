package org.multiverse.stms.alpha.instrumentation.fieldgranularity;

import org.multiverse.instrumentation.compiler.AbstractCompilePhase;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Environment;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.asm.AsmUtils.loadAsClassNode;
import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * @author Peter Veentjer
 */
public class FieldGranularityCompilePhase extends AbstractCompilePhase {

    public FieldGranularityCompilePhase() {
        super("FieldGranularityCompilePhase");
    }

    @Override
    protected Clazz doCompile(Environment environment, Clazz originalClazz) {
        ClassMetadata metadata = environment.getMetadataRepository().loadClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!metadata.hasFieldsWithFieldGranularity()) {
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
