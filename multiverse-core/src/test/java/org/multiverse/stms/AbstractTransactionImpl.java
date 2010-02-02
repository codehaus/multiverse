package org.multiverse.stms;

import org.multiverse.utils.backoff.ExponentialBackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.clock.StrictPrimitiveClock;

/**
 * @author Peter Veentjer
 */
public class AbstractTransactionImpl extends AbstractTransaction {

    public AbstractTransactionImpl() {
        super(new AbstractTransactionConfig(
                new StrictPrimitiveClock(1),
                ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                null,
                true,
                1000,
                true, true, true));
        init();
    }

    public AbstractTransactionImpl(PrimitiveClock clock) {
        super(new AbstractTransactionConfig(
                clock,
                ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                null,
                true,
                10000,
                true, true, true));
        init();
    }

    public AbstractTransactionImpl(String familyName, PrimitiveClock clock) {
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
