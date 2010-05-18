package org.multiverse.stms;

import org.multiverse.api.LogLevel;
import org.multiverse.api.TransactionConfiguration;
import org.multiverse.api.TransactionFactory;
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
    public final int maxRetries;
    public final boolean interruptible;
    public final boolean writeSkewAllowed;
    public final boolean readTrackingEnabled;
    public final long timeoutNs;
    public final boolean explicitRetryAllowed;
    public final int maxReadSpinCount;
    public final TransactionFactory transactionFactory;
    public final LogLevel logLevel;

    /**
     * This method should be removed, only used for testing purposes.
     */
    public AbstractTransactionConfiguration() {
        this(new StrictPrimitiveClock(), ExponentialBackoffPolicy.INSTANCE_100_MS_MAX,
                null, true, 1000, true, true, true, true, Long.MIN_VALUE, 10, null, LogLevel.none);
    }

    public AbstractTransactionConfiguration(
            PrimitiveClock clock, BackoffPolicy backoffPolicy, String familyName,
            boolean readOnly, int maxRetries, boolean interruptible,
            boolean writeSkewAllowed, boolean readTrackingEnabled,
            boolean explicitRetryAllowed, long timeoutNs, int maxReadSpinCount,
            TransactionFactory transactionFactory, LogLevel logLevel) {

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
        this.maxRetries = maxRetries;
        this.interruptible = interruptible;
        this.readTrackingEnabled = readTrackingEnabled;
        this.writeSkewAllowed = writeSkewAllowed;
        this.explicitRetryAllowed = explicitRetryAllowed;
        this.timeoutNs = timeoutNs;
        this.maxReadSpinCount = maxReadSpinCount;
        this.transactionFactory = transactionFactory;
        this.logLevel = logLevel;

        if (!readOnly && !readTrackingEnabled && !writeSkewAllowed) {
            String msg = format("Update transaction '%s' isn't  " +
                    "allowed with writeSkew " +
                    "disabled and trackReads disabled. " +
                    "The last is needed to do the first.",
                    familyName);
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public LogLevel getLogLevel() {
        return logLevel;
    }

    @Override
    public int maxReadSpinCount() {
        return maxReadSpinCount;
    }

    @Override
    public boolean isExplicitRetryAllowed() {
        return explicitRetryAllowed;
    }

    @Override
    public final String getFamilyName() {
        return familyName;
    }

    @Override
    public final int getMaxRetries() {
        return maxRetries;
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
    public final boolean isWriteSkewAllowed() {
        return writeSkewAllowed;
    }

    @Override
    public final boolean isReadTrackingEnabled() {
        return readTrackingEnabled;
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
