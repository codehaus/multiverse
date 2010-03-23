package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;

/**
 * @author Peter Veentjer
 */
public class ReadonlyAlphaTransactionConfig extends AbstractTransactionConfig {

    public final OptimalSize optimalSize;

    public ReadonlyAlphaTransactionConfig(
            PrimitiveClock clock, BackoffPolicy backoffPolicy,
            String familyName, OptimalSize optimalSize, int maxRetryCount,
            boolean interruptible, boolean automaticReadTracking) {

        super(clock, backoffPolicy, familyName, true, maxRetryCount, interruptible,
                true, automaticReadTracking);

        this.optimalSize = optimalSize;
    }
}
