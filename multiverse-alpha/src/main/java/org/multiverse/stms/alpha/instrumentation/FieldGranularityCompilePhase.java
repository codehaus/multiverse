package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.instrumentation.compiler.AbstractCompilePhase;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Environment;
import org.multiverse.stms.alpha.instrumentation.asm.FieldGranularityTransformer;
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
    protected Clazz doCompile(Environment environment, Clazz clazz) {
        ClassNode original = loadAsClassNode(clazz.getBytecode());

        FieldGranularityTransformer transformer = new FieldGranularityTransformer(
                clazz.getClassLoader(), original, environment.getMetadataRepository());

        ClassNode transformed = transformer.transform();

        return new Clazz(clazz, toBytecode(transformed));
    }
}
