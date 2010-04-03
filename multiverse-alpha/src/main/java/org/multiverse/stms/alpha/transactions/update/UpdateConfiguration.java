package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.stms.AbstractTransactionConfiguration;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;
import org.multiverse.utils.backoff.ExponentialBackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.commitlock.GenericCommitLockPolicy;

/**
 * {@link org.multiverse.api.TransactionConfiguration} for the {@link AbstractUpdateAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public final class UpdateConfiguration extends AbstractTransactionConfiguration {

    public final CommitLockPolicy commitLockPolicy;
    public final boolean optimizedConflictDetection;
    public final boolean dirtyCheck;
    public final SpeculativeConfiguration speculativeConfiguration;
    public final boolean quickReleaseLocks;

    public UpdateConfiguration(PrimitiveClock clock) {
        this(clock,
                ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                GenericCommitLockPolicy.FAIL_FAST_BUT_RETRY, null, new SpeculativeConfiguration(40),
                1000, true, true, true, true, true, true);
    }


    public UpdateConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy, CommitLockPolicy commitLockPolicy,
            String familyName, SpeculativeConfiguration speculativeConfiguration, int maxRetryCount,
            boolean interruptible, boolean automaticReadTracking, boolean allowWriteSkewProblem,
            boolean optimizedConflictDetection, boolean dirtyCheck, boolean quickReleaseLocks) {

        super(clock, backoffPolicy, familyName, false, maxRetryCount, interruptible,
                allowWriteSkewProblem, automaticReadTracking);

        this.commitLockPolicy = commitLockPolicy;
        this.optimizedConflictDetection = optimizedConflictDetection;
        this.dirtyCheck = dirtyCheck;
        this.speculativeConfiguration = speculativeConfiguration;
        this.quickReleaseLocks = quickReleaseLocks;
    }

    public UpdateConfiguration withSpeculativeConfiguration(SpeculativeConfiguration speculativeConfiguration) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, null, speculativeConfiguration, maxRetryCount, interruptible,
                automaticReadTracking, allowWriteSkewProblem, optimizedConflictDetection, dirtyCheck, quickReleaseLocks);
    }

    public UpdateConfiguration withAllowWriteSkewProblem(boolean allowWriteSkewProblem) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, null, speculativeConfiguration, maxRetryCount, interruptible,
                automaticReadTracking, allowWriteSkewProblem, optimizedConflictDetection, dirtyCheck, quickReleaseLocks);
    }

    public UpdateConfiguration withAutomaticReadTracking(boolean automaticReadTracking) {
        return new UpdateConfiguration(
                clock, backoffPolicy, commitLockPolicy, null, speculativeConfiguration, maxRetryCount, interruptible,
                automaticReadTracking, allowWriteSkewProblem, optimizedConflictDetection, dirtyCheck, quickReleaseLocks);
    }
}
