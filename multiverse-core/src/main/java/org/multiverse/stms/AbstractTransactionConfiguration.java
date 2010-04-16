package org.multiverse.stms;

import org.multiverse.api.TransactionConfiguration;
import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.backoff.ExponentialBackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.api.clock.StrictPrimitiveClock;

import static java.lang.String.format;

/**
 * Contains the configuration for the AbstractTransaction.
 * <p/>
 * One advantage of this class is that it is a lot easier to add additional fields without having to change all
 * constructors. It also reduces the need for constructors with a lot of arguments.`
 *
 * @author Peter Veentjer.
 */
public class AbstractTransactionConfiguration implements TransactionConfiguration {

    public final PrimitiveClock clock;
    public final BackoffPolicy backoffPolicy;
    public final String familyName;
    public final boolean readOnly;
    public final int maxRetryCount;
    public final boolean interruptible;
    public final boolean writeSkewProblemAllowed;
    public final boolean automaticReadTrackingEnabled;
    public final long timeoutNs;
    public final boolean explicitRetryAllowed;

    /**
     * This method should be removed, only used for testing purposes.
     */
    public AbstractTransactionConfiguration() {
        this(new StrictPrimitiveClock(), ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                null, true, 1000, true, true, true, true, Long.MIN_VALUE);
    }

    public AbstractTransactionConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy, String familyName,
            boolean readOnly, int maxRetryCount, boolean interruptible,
            boolean writeSkewProblemAllowed, boolean automaticReadTrackingEnabled,
            boolean explicitRetryAllowed, long timeoutNs) {

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
        this.automaticReadTrackingEnabled = automaticReadTrackingEnabled;
        this.writeSkewProblemAllowed = writeSkewProblemAllowed;
        this.explicitRetryAllowed = explicitRetryAllowed;
        this.timeoutNs = timeoutNs;

        if (!readOnly && !automaticReadTrackingEnabled && !writeSkewProblemAllowed) {
            String msg = format("Update transaction '%s' isn't  " +
                    "allowed with writeSkewProblemAllowed " +
                    "disabled and automaticReadTrackingEnabled disabled. " +
                    "The last is needed to do the first.",
                    familyName);
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public boolean isExplictRetryEnabled() {
        return explicitRetryAllowed;
    }

    @Override
    public final String getFamilyName() {
        return familyName;
    }

    @Override
    public final int getMaxRetryCount() {
        return maxRetryCount;
    }

    @Override
    public final boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public final boolean isReadonly() {
        return readOnly;
    }

    @Override
    public final boolean isWriteSkewProblemAllowed() {
        return writeSkewProblemAllowed;
    }

    @Override
    public final boolean isAutomaticReadTrackingEnabled() {
        return automaticReadTrackingEnabled;
    }

    @Override
    public final BackoffPolicy getBackoffPolicy() {
        return backoffPolicy;
    }

    @Override
    public long getTimeoutNs() {
        return timeoutNs;
    }
}
