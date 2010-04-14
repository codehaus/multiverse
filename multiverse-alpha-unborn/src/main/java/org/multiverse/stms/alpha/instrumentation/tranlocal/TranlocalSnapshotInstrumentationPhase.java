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
public class TranlocalSnapshotInstrumentationPhase extends AbstractInstrumentationPhase {

    public TranlocalSnapshotInstrumentationPhase() {
        super("TranlocalSnapshotInstrumentationPhase");
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        ClassMetadata classMetadata = environment.getMetadataRepository().loadClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!classMetadata.isRealTransactionalObject()) {
            return originalClazz;
        }

        ClassNode originalClassNode = loadAsClassNode(originalClazz.getBytecode());

        TranlocalSnapshotFactory factory = new TranlocalSnapshotFactory(
                originalClazz.getClassLoader(),
                originalClassNode,
                environment.getMetadataRepository());

        ClassNode result = factory.create();

        Clazz snapshotClazz = new Clazz(classMetadata.getTranlocalSnapshotName());
        snapshotClazz.setBytecode(toBytecode(result));
        snapshotClazz.setClassLoader(originalClazz.getClassLoader());
        environment.getFiler().createClassFile(snapshotClazz);

        return originalClazz;
    }
}
