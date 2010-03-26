package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.stms.AbstractTransactionConfiguration;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;
import org.multiverse.utils.clock.PrimitiveClock;

/**
 * @author Peter Veentjer
 */
public final class ReadonlyAlphaTransactionConfiguration extends AbstractTransactionConfiguration {

    public final SpeculativeConfiguration speculativeConfig;

    public ReadonlyAlphaTransactionConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy,
            String familyName, SpeculativeConfiguration speculativeConfig, int maxRetryCount,
            boolean interruptible, boolean automaticReadTracking) {

        super(clock, backoffPolicy, familyName, true, maxRetryCount, interruptible,
                true, automaticReadTracking);

        this.speculativeConfig = speculativeConfig;
    }
}
