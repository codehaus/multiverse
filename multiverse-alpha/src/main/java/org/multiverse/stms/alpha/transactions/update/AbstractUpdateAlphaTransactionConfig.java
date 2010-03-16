package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;

/**
 * {@link org.multiverse.api.TransactionConfig} for the {@link AbstractUpdateAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public class AbstractUpdateAlphaTransactionConfig extends AbstractTransactionConfig {

    public final CommitLockPolicy commitLockPolicy;
    public final boolean optimizedConflictDetection;
    public final boolean dirtyCheck;

    public AbstractUpdateAlphaTransactionConfig(
            PrimitiveClock clock, BackoffPolicy backoffPolicy, String familyName, boolean readOnly,
            int maxRetryCount, boolean interruptible, CommitLockPolicy commitLockPolicy,
            boolean allowWriteSkewProblem, boolean automaticReadTracking, boolean optimizedConflictDetection, boolean dirtyCheck) {

        super(clock,
                backoffPolicy, familyName, readOnly, maxRetryCount, interruptible, allowWriteSkewProblem, automaticReadTracking);

        this.commitLockPolicy = commitLockPolicy;
        this.optimizedConflictDetection = optimizedConflictDetection;
        this.dirtyCheck = dirtyCheck;
    }
}
