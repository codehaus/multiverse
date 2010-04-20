package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.multiverse.instrumentation.AbstractInstrumentationPhase;
import org.multiverse.instrumentation.Clazz;
import org.multiverse.instrumentation.Environment;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;

import static org.multiverse.instrumentation.asm.AsmUtils.loadAsClassNode;
import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * @author Peter Veentjer
 */
public final class TransactionalMethodInstrumentationPhase extends AbstractInstrumentationPhase {

    public TransactionalMethodInstrumentationPhase() {
        super("TransactionalMethodInstrumentationPhase");
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        ClassMetadata classMetadata = environment.getMetadataRepository().loadClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!classMetadata.hasTransactionalMethods()) {
            return originalClazz;
        }

        ClassNode original = loadAsClassNode(originalClazz.getBytecode());

        boolean restore = InsnList.check;
        InsnList.check = true;
        try {

            ClassNode donor = loadAsClassNode(TransactionLogicDonor.class);

            ClassNode result;
            if (classMetadata.isInterface()) {
                TransactionalInterfaceMethodTransformer transformer = new TransactionalInterfaceMethodTransformer(
                        originalClazz.getClassLoader(), original, environment.getMetadataRepository());
                result = transformer.transform();
            } else {
                TransactionalClassMethodTransformer transformer = new TransactionalClassMethodTransformer(
                        originalClazz.getClassLoader(), original, donor,
                        environment.getMetadataRepository(), environment.optimize(), environment.getLog());
                result = transformer.transform();
            }

            if (result == null) {
                return originalClazz;
            }

            byte[] newBytecode = toBytecode(result);
            return new Clazz(originalClazz, newBytecode);
        } finally {
            InsnList.check = restore;
        }
    }
}
