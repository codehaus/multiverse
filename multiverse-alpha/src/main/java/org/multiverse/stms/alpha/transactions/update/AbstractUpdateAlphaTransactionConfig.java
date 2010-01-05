package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

/**
 * {@link org.multiverse.api.TransactionConfig} for the {@link AbstractUpdateAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public class AbstractUpdateAlphaTransactionConfig extends AbstractTransactionConfig {

    public final CommitLockPolicy commitLockPolicy;
    public final ProfileRepository profiler;
    public final boolean optimizedConflictDetection;
    public final boolean dirtyCheck;

    public AbstractUpdateAlphaTransactionConfig(
            Clock clock, RestartBackoffPolicy restartBackoffPolicy, String familyName, boolean readOnly,
            int maxRetryCount, boolean interruptible, CommitLockPolicy commitLockPolicy, ProfileRepository profiler,
            boolean detectWriteSkew, boolean automaticReadTracking, boolean optimizedConflictDetection, boolean dirtyCheck) {

        super(clock, restartBackoffPolicy, familyName, readOnly, maxRetryCount, interruptible, detectWriteSkew, automaticReadTracking);

        this.profiler = profiler;
        this.commitLockPolicy = commitLockPolicy;
        this.optimizedConflictDetection = optimizedConflictDetection;
        this.dirtyCheck = dirtyCheck;
    }
}
