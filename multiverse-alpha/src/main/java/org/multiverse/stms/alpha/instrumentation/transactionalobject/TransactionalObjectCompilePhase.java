package org.multiverse.stms.alpha.instrumentation.transactionalobject;

import org.multiverse.instrumentation.compiler.AbstractCompilePhase;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Environment;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.asm.AsmUtils.loadAsClassNode;
import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * @author Peter Veentjer
 */
public class TransactionalObjectCompilePhase extends AbstractCompilePhase {

    public TransactionalObjectCompilePhase() {
        super("TransactionalObjectCompilePhase");
    }

    @Override
    protected Clazz doCompile(Environment environment, Clazz originalClazz) {
        ClassMetadata classMetadata = environment.getMetadataRepository().getClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!classMetadata.isRealTransactionalObject()) {
            return originalClazz;
        }

        ClassNode mixin = loadAsClassNode(DefaultTxObjectMixin.class);
        ClassNode original = loadAsClassNode(originalClazz.getBytecode());
        TransactionalObjectTransformer transformer = new TransactionalObjectTransformer(
                originalClazz.getClassLoader(), original, mixin, environment.getMetadataRepository());

        ClassNode result = transformer.transform();
        return new Clazz(originalClazz, toBytecode(result));
    }
}
