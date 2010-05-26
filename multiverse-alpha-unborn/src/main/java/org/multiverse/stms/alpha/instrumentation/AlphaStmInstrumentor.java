package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.instrumentation.PreventReinstrumentationInstrumentationPhase;
import org.multiverse.instrumentation.StandardInstrumentor;
import org.multiverse.instrumentation.asm.JSRInlineInstrumentationPhase;
import org.multiverse.stms.alpha.instrumentation.fieldaccess.NonTransactionalObjectFieldAccessInstrumentationPhase;
import org.multiverse.stms.alpha.instrumentation.fieldgranularity.FieldGranularityInstrumentationPhase;
import org.multiverse.stms.alpha.instrumentation.gettersetter.GetterSetterInlineInstrumentationPhase;
import org.multiverse.stms.alpha.instrumentation.tranlocal.TranlocalInstrumentationPhase;
import org.multiverse.stms.alpha.instrumentation.tranlocal.TranlocalSnapshotInstrumentationPhase;
import org.multiverse.stms.alpha.instrumentation.transactionalmethod.TransactionalMethodInstrumentationPhase;
import org.multiverse.stms.alpha.instrumentation.transactionalobject.TransactionalObjectInstrumentationPhase;

/**
 * The {@link org.multiverse.instrumentation.Instrumentor} for the Alpha Stm.
 *
 * @author Peter Veentjer
 */
public final class AlphaStmInstrumentor extends StandardInstrumentor {

    public AlphaStmInstrumentor() {
        super("AlphaStmInstrumentor", "0.5.2", "AlphaStm");

        add(new PreventReinstrumentationInstrumentationPhase(this));
        add(new JSRInlineInstrumentationPhase());
        add(new GetterSetterInlineInstrumentationPhase());
        add(new FieldGranularityInstrumentationPhase());
        add(new TranlocalInstrumentationPhase());
        add(new TranlocalSnapshotInstrumentationPhase());
        add(new NonTransactionalObjectFieldAccessInstrumentationPhase());
        add(new TransactionalObjectInstrumentationPhase());
        add(new TransactionalMethodInstrumentationPhase());
    }
}
