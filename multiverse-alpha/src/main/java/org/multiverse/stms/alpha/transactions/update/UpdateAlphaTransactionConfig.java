package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;

/**
 * {@link org.multiverse.api.TransactionConfig} for the {@link AbstractUpdateAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public class UpdateAlphaTransactionConfig extends AbstractTransactionConfig {

    public final CommitLockPolicy commitLockPolicy;
    public final boolean optimizedConflictDetection;
    public final boolean dirtyCheck;
    public final OptimalSize optimalSize;
   
    public UpdateAlphaTransactionConfig(
            PrimitiveClock clock, BackoffPolicy backoffPolicy,
            CommitLockPolicy commitLockPolicy, String familyName,OptimalSize optimalSize,
            int maxRetryCount, boolean interruptible, 
            boolean automaticReadTracking, boolean allowWriteSkewProblem,
            boolean optimizedConflictDetection, boolean dirtyCheck) {

        super(clock,backoffPolicy, familyName, false, maxRetryCount, interruptible,
                allowWriteSkewProblem, automaticReadTracking);

        this.commitLockPolicy = commitLockPolicy;
        this.optimizedConflictDetection = optimizedConflictDetection;
        this.dirtyCheck = dirtyCheck;
        this.optimalSize = optimalSize;
    }

}
