package org.multiverse.stms.alpha.transactions;

import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.stms.AbstractTransactionConfiguration;

/**
 * @author Peter Veentjer
 */
public class AbstractAlphaTransactionConfiguration extends AbstractTransactionConfiguration {

    public final SpeculativeConfiguration speculativeConfiguration;

    public AbstractAlphaTransactionConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy,
            String familyName, boolean readOnly, int maxRetryCount,
            boolean interruptible, boolean writeSkewProblemAllowed,
            boolean automaticReadTrackingEnabled, boolean explicitRetryAllowed,
            SpeculativeConfiguration speculativeConfiguration) {
        super(clock, backoffPolicy, familyName, readOnly, maxRetryCount, interruptible, writeSkewProblemAllowed, automaticReadTrackingEnabled, explicitRetryAllowed);

        this.speculativeConfiguration = speculativeConfiguration;
    }
}
