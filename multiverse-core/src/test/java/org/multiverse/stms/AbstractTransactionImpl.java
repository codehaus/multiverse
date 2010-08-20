package org.multiverse.stms;

import org.multiverse.api.PropagationLevel;
import org.multiverse.api.TraceLevel;
import org.multiverse.api.backoff.ExponentialBackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.api.clock.StrictPrimitiveClock;

/**
 * Implementation for testing purposes.
 *
 * @author Peter Veentjer
 */
public class AbstractTransactionImpl extends AbstractTransaction {

    public AbstractTransactionImpl() {
        super(new AbstractTransactionConfiguration(
                new StrictPrimitiveClock(1),
                ExponentialBackoffPolicy.INSTANCE_100_MS_MAX,
                null,
                true,
                1000,
                true, true, true, true, Long.MAX_VALUE, 10, null, TraceLevel.none, PropagationLevel.Requires));
    }

    public AbstractTransactionImpl(PrimitiveClock clock) {
        super(new AbstractTransactionConfiguration(
                clock,
                ExponentialBackoffPolicy.INSTANCE_100_MS_MAX,
                null,
                true,
                10000,
                true, true, true, true, Long.MAX_VALUE, 10, null, TraceLevel.none, PropagationLevel.Requires));
    }

    public AbstractTransactionImpl(String familyName, PrimitiveClock clock) {
        super(new AbstractTransactionConfiguration(
                clock,
                ExponentialBackoffPolicy.INSTANCE_100_MS_MAX,
                familyName,
                true,
                1000,
                true, true, true, true, Long.MAX_VALUE, 10, null, TraceLevel.none, PropagationLevel.Requires));
    }
}
