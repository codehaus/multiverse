package org.multiverse.stms.alpha;

import org.multiverse.stms.AbstractTransactionDependencies;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

/**
 * Contains the dependencies for the UpdateAlphaTransaction. The {@link org.multiverse.stms.alpha.AlphaStm} could be
 * seen as a dependency injection container for the UpdateAlphaTransaction.
 * <p/>
 * It also makes it a lot easier to add additional dependencies instead of having to update the constructor of the
 * UpdateAlphaTransaction.
 * <p/>
 * It also makes the UpdateTransaction smaller in memory size because there is only one reference needed to this
 * UpdateTransactionDependencies, instead of for each dependency individually. It does increase the stress on memory
 * because there is an extra field to traverse.
 * <p/>
 * All fields are final.
 *
 * @author Peter Veentjer.
 */
public class UpdateTransactionDependencies extends AbstractTransactionDependencies {

    public final ProfileRepository profiler;
    public final CommitLockPolicy commitLockPolicy;

    public UpdateTransactionDependencies(Clock clock, RestartBackoffPolicy restartBackoffPolicy,
                                         CommitLockPolicy commitLockPolicy, ProfileRepository profiler) {
        super(clock, restartBackoffPolicy);
        assert commitLockPolicy != null;
        this.profiler = profiler;
        this.commitLockPolicy = commitLockPolicy;
    }
}
