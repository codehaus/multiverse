package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.stms.AbstractTransactionConfiguration;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;

/**
 * {@link org.multiverse.api.TransactionConfiguration} for the {@link AbstractUpdateAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public final class UpdateAlphaTransactionConfiguration extends AbstractTransactionConfiguration {

    public final CommitLockPolicy commitLockPolicy;
    public final boolean optimizedConflictDetection;
    public final boolean dirtyCheck;
    public final SpeculativeConfiguration speculativeConfiguration;
    public final boolean quickReleaseLocks;

    public UpdateAlphaTransactionConfiguration(
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

}
