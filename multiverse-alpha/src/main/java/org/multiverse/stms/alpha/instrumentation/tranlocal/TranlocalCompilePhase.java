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
public class TranlocalCompilePhase extends AbstractCompilePhase {

    public TranlocalCompilePhase() {
        super("TranlocalCompilePhase");
    }

    @Override
    protected Clazz doCompile(Environment environment, Clazz originalClazz) {
        ClassLoader classLoader = originalClazz.getClassLoader();

        ClassMetadata classMetadata = environment.getMetadataRepository().loadClassMetadata(
                classLoader, originalClazz.getName());

        if (!classMetadata.isRealTransactionalObject()) {
            return originalClazz;
        }

        ClassNode originalClassNode = loadAsClassNode(originalClazz.getBytecode());

        TranlocalFactory transformer = new TranlocalFactory(
                classLoader, originalClassNode, environment.getMetadataRepository());
        ClassNode result = transformer.create();

        Clazz tranlocalClazz = new Clazz(classMetadata.getTranlocalName());
        tranlocalClazz.setBytecode(toBytecode(result));
        tranlocalClazz.setClassLoader(originalClazz.getClassLoader());

        environment.getFiler().createClassFile(tranlocalClazz);

        return originalClazz;
    }
}
