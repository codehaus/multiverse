package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.backoff.ExponentialBackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.stms.alpha.transactions.AbstractAlphaTransactionConfiguration;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

/**
 * @author Peter Veentjer
 */
public final class ReadonlyConfiguration extends AbstractAlphaTransactionConfiguration {

    public ReadonlyConfiguration(PrimitiveClock clock, boolean automaticReadTrackingEnabled) {
        this(clock, ExponentialBackoffPolicy.INSTANCE_10_MS_MAX, null, new SpeculativeConfiguration(100), 1000,
                true, automaticReadTrackingEnabled, true);
    }

    public ReadonlyConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy,
            String familyName, SpeculativeConfiguration speculativeConfig, int maxRetryCount,
            boolean interruptible, boolean automaticReadTracking, boolean explicitRetryAllowed) {

        super(clock, backoffPolicy, familyName, true, maxRetryCount, interruptible,
                true, automaticReadTracking, explicitRetryAllowed, speculativeConfig);
    }

    public ReadonlyConfiguration withSpeculativeConfig(SpeculativeConfiguration speculativeConfig) {
        return new ReadonlyConfiguration(clock, backoffPolicy, familyName,
                speculativeConfig, maxRetryCount, interruptible, automaticReadTrackingEnabled,
                explicitRetryAllowed);
    }

    public ReadonlyConfiguration withExplicitRetryAllowed(boolean explicitRetryAllowed) {
        return new ReadonlyConfiguration(clock, backoffPolicy, familyName,
                speculativeConfiguration, maxRetryCount, interruptible, automaticReadTrackingEnabled,
                explicitRetryAllowed);
    }
}
