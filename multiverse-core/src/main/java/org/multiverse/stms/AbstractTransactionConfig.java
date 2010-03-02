package org.multiverse.stms;

import org.multiverse.api.TransactionConfig;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.backoff.ExponentialBackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.clock.StrictPrimitiveClock;

import java.util.concurrent.TimeUnit;

/**
 * Contains the configuration for the AbstractTransaction.
 * <p/>
 * One advantage of this class is that it is a lot easier to add additional fields without having to change all
 * constructors. It also reduces the need for constructors with a lot of arguments.`
 *
 * @author Peter Veentjer.
 */
public class AbstractTransactionConfig implements TransactionConfig {

    public final PrimitiveClock clock;
    public final BackoffPolicy backoffPolicy;
    public final String familyName;
    public final boolean readOnly;
    public final int maxRetryCount;
    public final boolean interruptible;
    public final boolean allowWriteSkewProblem;
    public final boolean automaticReadTracking;
    private long timeout;
    private TimeUnit timeoutTimeUnit;

    /**
     * This method should be removed, only used for testing purposes.
     */
    public AbstractTransactionConfig() {
        this(new StrictPrimitiveClock(), ExponentialBackoffPolicy.INSTANCE_10_MS_MAX, null, true, 1000, true, true, true);
    }

    public AbstractTransactionConfig(
            PrimitiveClock clock, BackoffPolicy backoffPolicy, String familyName, boolean readOnly,
            int maxRetryCount, boolean interruptible, boolean allowWriteSkewProblem, boolean automaticReadTracking) {

        if (clock == null) {
            throw new NullPointerException();
        }

        if (backoffPolicy == null) {
            throw new NullPointerException();
        }

        this.clock = clock;
        this.familyName = familyName;
        this.readOnly = readOnly;
        this.backoffPolicy = backoffPolicy;
        this.maxRetryCount = maxRetryCount;
        this.interruptible = interruptible;
        this.automaticReadTracking = automaticReadTracking;
        this.allowWriteSkewProblem = allowWriteSkewProblem;

        if (!readOnly && !automaticReadTracking && !allowWriteSkewProblem) {
            throw new IllegalArgumentException("It isn't allowed to have a update transaction with allowWriteSkewProblem " +
                    "disabled and automaticReadTracking disabled. The last is needed to do the first.");
        }
    }

    @Override
    public String getFamilyName() {
        return familyName;
    }

    @Override
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public boolean isReadonly() {
        return readOnly;
    }

    @Override
    public boolean allowWriteSkewProblem() {
        return allowWriteSkewProblem;
    }

    @Override
    public boolean automaticReadTracking() {
        return automaticReadTracking;
    }

    @Override
    public BackoffPolicy getBackoffPolicy() {
        return backoffPolicy;
    }


}
