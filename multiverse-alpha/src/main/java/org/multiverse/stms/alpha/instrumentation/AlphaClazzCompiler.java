package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.instrumentation.asm.JSRInlineCompilePhase;
import org.multiverse.instrumentation.compiler.StandardClazzCompiler;
import org.multiverse.stms.alpha.instrumentation.fieldaccess.NonTransactionalObjectFieldAccessCompilePhase;
import org.multiverse.stms.alpha.instrumentation.fieldgranularity.FieldGranularityCompilePhase;
import org.multiverse.stms.alpha.instrumentation.tranlocal.TranlocalCompilePhase;
import org.multiverse.stms.alpha.instrumentation.tranlocal.TranlocalSnapshotCompilePhase;
import org.multiverse.stms.alpha.instrumentation.transactionalmethod.TransactionalMethodCompilePhase;
import org.multiverse.stms.alpha.instrumentation.transactionalobject.TransactionalObjectCompilePhase;

/**
 * The {@link org.multiverse.instrumentation.compiler.ClazzCompiler} for the Alpha Stm.
 *
 * @author Peter Veentjer
 */
public class AlphaClazzCompiler extends StandardClazzCompiler {

    public AlphaClazzCompiler() {
        super("AlphaClazzCompiler", "0.5-SNAPSHOT");

        add(new JSRInlineCompilePhase());
        add(new FieldGranularityCompilePhase());
        add(new TranlocalCompilePhase());
        add(new TranlocalSnapshotCompilePhase());
        add(new NonTransactionalObjectFieldAccessCompilePhase());
        add(new TransactionalObjectCompilePhase());
        add(new TransactionalMethodCompilePhase());
    }
}
