package org.multiverse.stms.alpha;

import org.multiverse.stms.AbstractTransactionDependencies;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

public class ReadonlyAlphaTransactionDependencies extends AbstractTransactionDependencies{

    public final ProfileRepository profiler;

    public ReadonlyAlphaTransactionDependencies(Clock clock, RestartBackoffPolicy restartBackoffPolicy, ProfileRepository profiler) {
        super(clock, restartBackoffPolicy);
        this.profiler = profiler;
    }
}
