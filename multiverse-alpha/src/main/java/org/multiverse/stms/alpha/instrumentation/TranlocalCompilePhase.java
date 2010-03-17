package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.instrumentation.compiler.AbstractCompilePhase;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Environment;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.stms.alpha.instrumentation.asm.TranlocalFactory;
import org.objectweb.asm.tree.ClassNode;

import static org.multiverse.instrumentation.ClassUtils.defineClass;
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

        ClassMetadata classMetadata = environment.getMetadataRepository().getClassMetadata(
                classLoader, originalClazz.getName());

        if (!classMetadata.isRealTransactionalObject()) {
            return originalClazz;
        }

        ClassNode original = loadAsClassNode(originalClazz.getBytecode());
        TranlocalFactory transformer = new TranlocalFactory(
                classLoader, original, environment.getMetadataRepository());
        ClassNode result = transformer.create();

        byte[] resultBytecode = toBytecode(result);

        //todo: not here
        defineClass(classLoader, result.name, resultBytecode);


        return originalClazz;
    }
}
