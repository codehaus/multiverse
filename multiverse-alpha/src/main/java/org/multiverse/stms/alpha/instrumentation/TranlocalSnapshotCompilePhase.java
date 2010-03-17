package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.instrumentation.compiler.AbstractCompilePhase;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Environment;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.stms.alpha.instrumentation.asm.TranlocalSnapshotFactory;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.ClassUtils.defineClass;
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
        ClassMetadata classMetadata = environment.getMetadataRepository().getClassMetadata(
                originalClazz.getClassLoader(), originalClazz.getName());

        if (!classMetadata.isRealTransactionalObject()) {
            return originalClazz;
        }

        ClassNode original = loadAsClassNode(originalClazz.getBytecode());
        TranlocalSnapshotFactory factory = new TranlocalSnapshotFactory(
                originalClazz.getClassLoader(), original, environment.getMetadataRepository());

        ClassNode result = factory.create();

        //todo: this should not be done here.
        byte[] resultBytecode = toBytecode(result);
        defineClass(originalClazz.getClassLoader(), result.name, resultBytecode);

        return originalClazz;
    }
}
