package org.multiverse.api;

import org.multiverse.api.backoff.BackoffPolicy;

/**
 * Contains the transaction configuration used by a {@link Transaction}. In the beginning this was all
 * placed in the Transaction, adding a lot of 'informational' methods to the transaction and therefor
 * complicating its usage. So all the configurational properties of the transaction are contained in
 * this structure.
 *
 * @author Peter Veentjer.
 */
public interface TransactionConfiguration {

    /**
     * Returns the TraceLevel
     *
     * @return the TraceLevel.
     */
    TraceLevel getTraceLevel();

    /**
     * Returns the BackoffPolicy used by the Stm when a transaction conflicts with another transaction.
     *
     * @return the BackoffPolicy used.
     * @see TransactionFactoryBuilder#setBackoffPolicy(BackoffPolicy)
     */
    BackoffPolicy getBackoffPolicy();

    /**
     * Returns the family name of this Transaction. Every transaction in principle should have a family name. This
     * information can be used for debugging/logging purposes but also other techniques that rely to know something
     * about similar types of transactions like profiling.
     *
     * @return the familyName. The returned value can be null.
     * @see TransactionFactoryBuilder#setFamilyName(String)
     */
    String getFamilyName();

    /**
     * Returns the maximum number of times this Transaction be retried before failing. The returned value will
     * always be equal or larger than 0. If the value is set high and you are encountering a lot of
     * TooManyRetryExceptions it could be that the objects are just not concurrent enough.
     *
     * @return the maxRetries.
     * @see TransactionFactoryBuilder#setMaxRetries(int)
     */
    int getMaxRetries();

    /**
     * Returns the total timeout in nanoseconds. Long.MAX_VALUE indicates that there is no
     * timeout.
     *
     * @return the total remaining timeout.
     * @see TransactionFactoryBuilder#setTimeoutNs(long)
     */
    long getTimeoutNs();

    /**
     * If an explicit retry (so a blocking transaction) is allowed. With this property one can prevent
     * that a Transaction is able to block waiting for some change.
     *
     * @return true if explicit retry is allowed, false otherwise.
     * @see TransactionFactoryBuilder#setExplicitRetryAllowed(boolean)
     */
    boolean isExplicitRetryAllowed();

    /**
     * Checks if the Transaction can be interrupted if it is blocking.
     *
     * @return true if the Transaction can be interrupted if it is blocking, false otherwise.
     * @see TransactionFactoryBuilder#setInterruptible(boolean)
     */
    boolean isInterruptible();

    /**
     * Checks if this Transaction is readonly.
     *
     * @return true if readonly, false otherwise.
     * @see TransactionFactoryBuilder#setReadonly(boolean)
     */
    boolean isReadonly();

    /**
     * Checks if this transaction does automatic read tracking. Read tracking is needed for blocking transactions,
     * but also for writeskew detection. Disadvantage of read tracking is that it is more expensive because
     * the reads not to be registered on some datastructure so that they are tracked.
     *
     * @return true if the transaction does automatic read tracking, false otherwise.
     * @see TransactionFactoryBuilder#setReadTrackingEnabled(boolean)
     */
    boolean isReadTrackingEnabled();

    /**
     * Checks if this Transaction allows writeskew. This is an isolation anomaly and could lead to an execution
     * of transactions that doesn't match any sequential execution. Writeskew detection can be expensive because
     * more checks needs to be done. It also leads to lower concurrency because certain executions of transactions
     * are not allowed and are aborted and retried.
     * <p/>
     * If the transaction is readonly, the value is undefined since a readonly transaction can't suffer from the
     * writeskew problem.
     *
     * @return true if the writeskew problem is allowed, false otherwise.
     * @see TransactionFactoryBuilder#setWriteSkewAllowed(boolean)
     */
    boolean isWriteSkewAllowed();

    /**
     * Returns the maximum number of times the transaction is allowed to spin on a read to become
     * readable (perhaps it is locked).
     *
     * @return the maximum number of spins
     */
    int maxReadSpinCount();
}
