package org.multiverse.stms.alpha.instrumentation.tranlocal;

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
public class TranlocalSnapshotCompilePhase extends AbstractCompilePhase {

    public TranlocalSnapshotCompilePhase() {
        super("TranlocalSnapshotCompilePhase");
    }

    @Override
    protected Clazz doCompile(Environment environment, Clazz originalClazz) {
        ClassMetadata classMetadata = environment.getMetadataRepository().loadClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!classMetadata.isRealTransactionalObject()) {
            return originalClazz;
        }

        ClassNode original = loadAsClassNode(originalClazz.getBytecode());
        TranlocalSnapshotFactory factory = new TranlocalSnapshotFactory(
                originalClazz.getClassLoader(), original, environment.getMetadataRepository());

        ClassNode result = factory.create();

        Clazz snapshotClazz = new Clazz(classMetadata.getTranlocalSnapshotName());
        snapshotClazz.setBytecode(toBytecode(result));
        snapshotClazz.setClassLoader(originalClazz.getClassLoader());
        environment.getFiler().createClassFile(snapshotClazz);

        return originalClazz;
    }
}
