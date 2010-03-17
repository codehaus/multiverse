package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.instrumentation.compiler.StandardClazzCompiler;

/**
 * This compiler should be used for the Javaagent and also for
 *
 * @author Peter Veentjer
 */
public class AlphaClazzCompiler extends StandardClazzCompiler {

    public AlphaClazzCompiler() {
        super("AlphaClazzCompiler");
        add(new JSRInlineCompilePhase());
        add(new FieldGranularityCompilePhase());
        add(new TranlocalCompilePhase());
        add(new TranlocalSnapshotCompilePhase());
        add(new NonTransactionalObjectFieldAccessCompilePhase());
        add(new TransactionalObjectCompilePhase());
        add(new TransactionalMethodCompilePhase());
    }
}
