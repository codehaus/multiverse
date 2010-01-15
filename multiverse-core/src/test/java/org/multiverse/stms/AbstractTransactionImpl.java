package org.multiverse.stms;

import org.multiverse.utils.backoff.ExponentialBackoffPolicy;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.clock.StrictClock;

/**
 * @author Peter Veentjer
 */
public class AbstractTransactionImpl extends AbstractTransaction {

    public AbstractTransactionImpl() {
        super(new AbstractTransactionConfig(
                new StrictClock(1),
                ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                null,
                true,
                1000,
                true, true, true));
        init();
    }

    public AbstractTransactionImpl(Clock clock) {
        super(new AbstractTransactionConfig(
                clock,
                ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                null,
                true,
                10000,
                true, true, true));
        init();
    }

    public AbstractTransactionImpl(String familyName, Clock clock) {
        super(new AbstractTransactionConfig(
                clock,
                ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                familyName,
                true,
                1000,
                true, true, true));

        init();
    }
}
