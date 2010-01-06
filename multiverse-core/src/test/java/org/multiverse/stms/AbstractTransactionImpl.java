package org.multiverse.stms;

import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.clock.StrictClock;
import org.multiverse.utils.restartbackoff.ExponentialRestartBackoffPolicy;

/**
 * @author Peter Veentjer
 */
public class AbstractTransactionImpl extends AbstractTransaction {

    public AbstractTransactionImpl() {
        super(new AbstractTransactionDependencies(
                new StrictClock(1), ExponentialRestartBackoffPolicy.INSTANCE_10_MS_MAX), null);
        init();
    }

    public AbstractTransactionImpl(Clock clock) {
        super(new AbstractTransactionDependencies(
                clock, ExponentialRestartBackoffPolicy.INSTANCE_10_MS_MAX), null);
        init();
    }

    public AbstractTransactionImpl(String familyName, Clock clock) {
        super(new AbstractTransactionDependencies(
                clock, ExponentialRestartBackoffPolicy.INSTANCE_10_MS_MAX), familyName);
        init();
    }
}
