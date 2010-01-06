package org.multiverse.stms;

import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.clock.StrictClock;
import org.multiverse.utils.restartbackoff.ExponentialRestartBackoffPolicy;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

/**
 * Contains the dependencies for the AbstractTransaction which don't have to be transaction family specific. So the
 * transactionName is not part of the dependencies.
 * <p/>
 * One advantage of this class is that it is a lot easier to add additional fields without having to change all
 * constructors. It also reduces the need for constructors with a lot of arguments.`
 *
 * @author Peter Veentjer.
 */
public class AbstractTransactionDependencies {

    public final Clock clock;
    public final RestartBackoffPolicy restartBackoffPolicy;

    public AbstractTransactionDependencies() {
        this(new StrictClock(), ExponentialRestartBackoffPolicy.INSTANCE_10_MS_MAX);
    }

    public AbstractTransactionDependencies(Clock clock, RestartBackoffPolicy restartBackoffPolicy) {
        assert clock != null;
        assert restartBackoffPolicy != null;
        this.clock = clock;
        this.restartBackoffPolicy = restartBackoffPolicy;
    }
}
