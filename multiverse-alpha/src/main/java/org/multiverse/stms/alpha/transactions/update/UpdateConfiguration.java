package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.backoff.ExponentialBackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.api.commitlock.CommitLockPolicy;
import org.multiverse.api.commitlock.GenericCommitLockPolicy;
import org.multiverse.stms.AbstractTransactionConfiguration;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

/**
 * {@link org.multiverse.api.TransactionConfiguration} for the {@link AbstractUpdateAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public final class UpdateConfiguration extends AbstractTransactionConfiguration {

    public final CommitLockPolicy commitLockPolicy;
    public final boolean optimizedConflictDetectionEnabled;
    public final boolean dirtyCheckEnabled;
    public final SpeculativeConfiguration speculativeConfiguration;
    public final boolean quickReleaseLocksEnabled;

    public UpdateConfiguration(PrimitiveClock clock) {
        this(clock,
                ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                GenericCommitLockPolicy.FAIL_FAST_BUT_RETRY, null, new SpeculativeConfiguration(40),
                1000, true, true, true, true, true, true, true);
    }

    public UpdateConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy, CommitLockPolicy commitLockPolicy,
            String familyName, SpeculativeConfiguration speculativeConfiguration, int maxRetryCount,
            boolean interruptible, boolean automaticReadTracking, boolean allowWriteSkewProblem,
            boolean optimizedConflictDetectionEnabled, boolean dirtyCheckEnabled, boolean quickReleaseLocks,
            boolean explicitRetryAllowed) {

        super(clock, backoffPolicy, familyName, false, maxRetryCount, interruptible,
                allowWriteSkewProblem, automaticReadTracking, explicitRetryAllowed);

        this.commitLockPolicy = commitLockPolicy;
        this.optimizedConflictDetectionEnabled = optimizedConflictDetectionEnabled;
        this.dirtyCheckEnabled = dirtyCheckEnabled;
        this.speculativeConfiguration = speculativeConfiguration;
        this.quickReleaseLocksEnabled = quickReleaseLocks;
    }

    public UpdateConfiguration withSpeculativeConfiguration(SpeculativeConfiguration speculativeConfigurationEnabled) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, null, speculativeConfigurationEnabled,
                maxRetryCount, interruptible, automaticReadTrackingEnabled, writeSkewProblemAllowed,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled, explicitRetryAllowed);
    }

    public UpdateConfiguration withWriteSkewProblemAllowed(boolean allowWriteSkewProblem) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, null, speculativeConfiguration,
                maxRetryCount, interruptible, automaticReadTrackingEnabled, allowWriteSkewProblem,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled, explicitRetryAllowed);
    }

    public UpdateConfiguration withAutomaticReadTrackingEnabled(boolean automaticReadTrackingEnabled) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, null, speculativeConfiguration,
                maxRetryCount, interruptible, automaticReadTrackingEnabled, writeSkewProblemAllowed,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled, explicitRetryAllowed);
    }

    public UpdateConfiguration withExplictRetryAllowed(boolean explicitRetryAllowed) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, null, speculativeConfiguration,
                maxRetryCount, interruptible, automaticReadTrackingEnabled, writeSkewProblemAllowed,
                optimizedConflictDetectionEnabled, dirtyCheckEnabled, quickReleaseLocksEnabled, explicitRetryAllowed);
    }
}
