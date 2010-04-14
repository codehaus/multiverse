package org.multiverse.stms.alpha.instrumentation.transactionalobject;

import org.multiverse.instrumentation.AbstractInstrumentationPhase;
import org.multiverse.instrumentation.Clazz;
import org.multiverse.instrumentation.Environment;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.asm.AsmUtils.loadAsClassNode;
import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * @author Peter Veentjer
 */
public class TransactionalObjectInstrumentationPhase extends AbstractInstrumentationPhase {

    public TransactionalObjectInstrumentationPhase() {
        super("TransactionalObjectInstrumentationPhase");
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        ClassMetadata classMetadata = environment.getMetadataRepository().loadClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!classMetadata.isRealTransactionalObject()) {
            environment.getLog().lessImportant("%s is not a real transactional object", originalClazz.getName());
            return originalClazz;
        }

        environment.getLog().lessImportant("%s is a real transactional object", originalClazz.getName());
        ClassNode mixinClassNode = loadAsClassNode(DefaultTxObjectMixin.class);
        ClassNode originalClassNode = loadAsClassNode(originalClazz.getBytecode());

        TransactionalObjectTransformer transformer = new TransactionalObjectTransformer(
                originalClazz.getClassLoader(), originalClassNode, mixinClassNode, environment.getMetadataRepository());

        ClassNode result = transformer.transform();
        return new Clazz(originalClazz, toBytecode(result));
    }
}
