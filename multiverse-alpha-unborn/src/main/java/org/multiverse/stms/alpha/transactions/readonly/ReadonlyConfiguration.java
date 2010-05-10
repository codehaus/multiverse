package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.TransactionFactory;
import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.backoff.ExponentialBackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.stms.alpha.transactions.AbstractAlphaTransactionConfiguration;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

/**
 * @author Peter Veentjer
 */
public final class ReadonlyConfiguration extends AbstractAlphaTransactionConfiguration {

    public ReadonlyConfiguration(PrimitiveClock clock, boolean readTrackingEnabled) {
        this(clock, ExponentialBackoffPolicy.INSTANCE_100_MS_MAX, null, new SpeculativeConfiguration(100), 1000,
                true, readTrackingEnabled, true, Long.MAX_VALUE, 10, null);
    }

    public ReadonlyConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy, String familyName,
            SpeculativeConfiguration speculativeConfig, int maxRetries, boolean interruptible,
            boolean readTrackingEnabled, boolean explicitRetryAllowed, long timeoutNs,
            int maxReadSpinCount, TransactionFactory transactionFactory) {

        super(clock, backoffPolicy, familyName, true, maxRetries, interruptible,
                true, readTrackingEnabled, explicitRetryAllowed, speculativeConfig, timeoutNs, maxReadSpinCount, transactionFactory);
    }

    public ReadonlyConfiguration withMaxRetries(int maxRetries) {
        return new ReadonlyConfiguration(clock, backoffPolicy, familyName,
                speculativeConfiguration, maxRetries, interruptible, readTrackingEnabled,
                explicitRetryAllowed, timeoutNs, maxReadSpinCount, transactionFactory);
    }

    public ReadonlyConfiguration withSpeculativeConfig(SpeculativeConfiguration speculativeConfig) {
        return new ReadonlyConfiguration(clock, backoffPolicy, familyName,
                speculativeConfig, maxRetries, interruptible, readTrackingEnabled,
                explicitRetryAllowed, timeoutNs, maxReadSpinCount, transactionFactory);
    }

    public ReadonlyConfiguration withExplicitRetryAllowed(boolean explicitRetryAllowed) {
        return new ReadonlyConfiguration(clock, backoffPolicy, familyName,
                speculativeConfiguration, maxRetries, interruptible, readTrackingEnabled,
                explicitRetryAllowed, timeoutNs, maxReadSpinCount, transactionFactory);
    }
}
