package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.PropagationLevel;
import org.multiverse.api.TraceLevel;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.backoff.ExponentialBackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.api.commitlock.CommitLockPolicy;
import org.multiverse.api.commitlock.GenericCommitLockPolicy;
import org.multiverse.stms.alpha.transactions.AbstractAlphaTransactionConfiguration;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

/**
 * {@link org.multiverse.api.TransactionConfiguration} for the {@link AbstractUpdateAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public final class UpdateConfiguration extends AbstractAlphaTransactionConfiguration {

    public final CommitLockPolicy commitLockPolicy;
    public final boolean optimizedConflictDetectionEnabled;
    public final boolean dirtyCheckEnabled;
    public final boolean quickReleaseLocksEnabled;

    public UpdateConfiguration(PrimitiveClock clock) {
        this(clock,
                ExponentialBackoffPolicy.INSTANCE_100_MS_MAX,
                GenericCommitLockPolicy.FAIL_FAST_BUT_RETRY, null, new SpeculativeConfiguration(40),
                1000, true, true, true, true, true, true, true, Long.MAX_VALUE,
                10, null, TraceLevel.none,0, PropagationLevel.Requires);
    }

    public UpdateConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy, CommitLockPolicy commitLockPolicy,
            String familyName, SpeculativeConfiguration speculativeConfiguration, int maxRetries,
            boolean interruptible, boolean automaticReadTracking, boolean writeSkewAllowed,
            boolean optimizedConflictDetectionEnabled, boolean dirtyCheckEnabled, boolean quickReleaseLocks,
            boolean explicitRetryAllowed, long timeoutNs, int maxReadSpinCount,
            TransactionFactory transactionFactory, TraceLevel traceLevel, int syncToClock,
            PropagationLevel propagationLevel) {

        super(clock, backoffPolicy, familyName, false, maxRetries, interruptible,
                writeSkewAllowed, automaticReadTracking, explicitRetryAllowed,
                speculativeConfiguration, timeoutNs, maxReadSpinCount, transactionFactory,
                traceLevel, syncToClock, propagationLevel);

        this.commitLockPolicy = commitLockPolicy;
        this.optimizedConflictDetectionEnabled = optimizedConflictDetectionEnabled;
        this.dirtyCheckEnabled = dirtyCheckEnabled;
        this.quickReleaseLocksEnabled = quickReleaseLocks;
    }

    public UpdateConfiguration withSpeculativeConfiguration(SpeculativeConfiguration speculativeConfigurationEnabled) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfigurationEnabled,
                maxRetries, interruptible, readTrackingEnabled, writeSkewAllowed,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled,
                explicitRetryAllowed, timeoutNs, maxReadSpinCount, transactionFactory,
                traceLevel, syncToClock,propagationLevel);
    }

    public UpdateConfiguration withWriteSkewAllowed(boolean writeSkewAllowed) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfiguration,
                maxRetries, interruptible, readTrackingEnabled, writeSkewAllowed,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled,
                explicitRetryAllowed, timeoutNs, maxReadSpinCount, transactionFactory,
                traceLevel, syncToClock,propagationLevel);
    }

    public UpdateConfiguration withReadTrackingEnabled(boolean readTrackingEnabled) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfiguration,
                maxRetries, interruptible, readTrackingEnabled, writeSkewAllowed,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled,
                explicitRetryAllowed, timeoutNs, maxReadSpinCount, transactionFactory,
                traceLevel, syncToClock,propagationLevel);
    }

    public UpdateConfiguration withExplictRetryAllowed(boolean explicitRetryAllowed) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfiguration,
                maxRetries, interruptible, readTrackingEnabled, writeSkewAllowed,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled,
                explicitRetryAllowed, timeoutNs, maxReadSpinCount, transactionFactory,
                traceLevel, syncToClock, propagationLevel);
    }

    public UpdateConfiguration withMaxRetries(int maxRetries) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfiguration,
                maxRetries, interruptible, readTrackingEnabled, writeSkewAllowed,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled,
                explicitRetryAllowed, timeoutNs, maxReadSpinCount, transactionFactory,
                traceLevel, syncToClock,propagationLevel);
    }
}
