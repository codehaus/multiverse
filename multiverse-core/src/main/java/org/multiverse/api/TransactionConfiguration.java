package org.multiverse.api;

/**
 * Contains the transaction configuration used by a {@link Transaction}. In the beginning this was all placed in the
 * Transaction, adding a lot of 'informational' methods to the transaction and therefor complicating its usage. So
 * all the configurational stuff was moved to a specialized object: the TransactionConfiguration.
 *
 * @author Peter Veentjer.
 */
public interface TransactionConfiguration {

    /**
     * Returns the BackoffPolicy used by the Stm when a transaction conflicts with another transaction.
     *
     * @return the BackoffPolicy used.
     */
    BackoffPolicy getBackoffPolicy();

    /**
     * Checks if this transaction does automaticReadTracking. Read tracking is needed for blocking transactions,
     * but also for writeskew detection. Disadvantage of read tracking is that it is more expensive because
     * the reads not to be registered on some datastructure so that they are tracked.
     *
     * @return true if the transaction does automatic read tracking, false otherwise.
     */
    boolean automaticReadTracking();

    /**
     * Returns the family name of this Transaction. Every transaction in principle should have a family name. This
     * information can be used for debugging/logging purposes but also other techniques that rely to know something
     * about similar types of transactions like profiling.
     *
     * @return the familyName. The returned value can be null.
     */
    String getFamilyName();

    /**
     * Returns the maximum number of times this Transaction be retried before failing. The returned value will
     * always be equal or larger than 0. If the value is set high and you are encountering a lot of
     * TooManyRetryExceptions it could be that the objects are just not concurrent enough.
     *
     * @return the maxRetryCount.
     */
    int getMaxRetryCount();

    /**
     * Checks if the Transaction can be interrupted if it is blocking.
     *
     * @return true if the Transaction can be interrupted if it is blocking, false otherwise.
     */
    boolean isInterruptible();

    /**
     * Checks if this Transaction is readonly.
     *
     * @return true if readonly, false otherwise.
     */
    boolean isReadonly();

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
     */
    boolean allowWriteSkewProblem();

}
