package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.stms.AbstractTransactionConfiguration;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;
import org.multiverse.utils.backoff.ExponentialBackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;

/**
 * @author Peter Veentjer
 */
public final class ReadonlyConfiguration extends AbstractTransactionConfiguration {

    public final SpeculativeConfiguration speculativeConfig;

    public ReadonlyConfiguration(PrimitiveClock clock) {
        this(clock, ExponentialBackoffPolicy.INSTANCE_10_MS_MAX, null, new SpeculativeConfiguration(100), 1000,
                true, true);
    }

    public ReadonlyConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy,
            String familyName, SpeculativeConfiguration speculativeConfig, int maxRetryCount,
            boolean interruptible, boolean automaticReadTracking) {

        super(clock, backoffPolicy, familyName, true, maxRetryCount, interruptible,
                true, automaticReadTracking);

        this.speculativeConfig = speculativeConfig;
    }

    public ReadonlyConfiguration withSpeculativeConfig(SpeculativeConfiguration speculativeConfig) {
        return new ReadonlyConfiguration(clock, backoffPolicy, familyName,
                speculativeConfig, maxRetryCount, interruptible, automaticReadTracking);
    }

    public ReadonlyConfiguration withAutomaticReadTracking(boolean automaticReadTracking) {
        return new ReadonlyConfiguration(clock, backoffPolicy, familyName,
                speculativeConfig, maxRetryCount, interruptible, automaticReadTracking);
    }

}
