package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.multiverse.instrumentation.compiler.AbstractCompilePhase;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Environment;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;

import static org.multiverse.instrumentation.asm.AsmUtils.loadAsClassNode;
import static org.multiverse.instrumentation.asm.AsmUtils.toBytecode;

/**
 * @author Peter Veentjer
 */
public class TransactionalMethodCompilePhase extends AbstractCompilePhase {

    public TransactionalMethodCompilePhase() {
        super("TransactionalMethodCompilePhase");
    }

    @Override
    protected Clazz doCompile(Environment environment, Clazz originalClazz) {
        ClassMetadata classMetadata = environment.getMetadataRepository().loadClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!classMetadata.hasTransactionalMethods()) {
            return originalClazz;
        }

        boolean restore = InsnList.check;
        InsnList.check = true;
        try {

            ClassNode original = loadAsClassNode(originalClazz.getBytecode());
            ClassNode donor = loadAsClassNode(TransactionLogicDonor.class);

            ClassNode result;
            if (classMetadata.isInterface()) {
                TransactionalInterfaceMethodTransformer transformer = new TransactionalInterfaceMethodTransformer(
                        originalClazz.getClassLoader(), original, environment.getMetadataRepository());
                result = transformer.transform();
            } else {
                TransactionalClassMethodTransformer transformer = new TransactionalClassMethodTransformer(
                        originalClazz.getClassLoader(), original, donor, environment.getMetadataRepository());
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
