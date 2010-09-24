package org.multiverse.api;

import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import java.util.List;

/**
 * Contains the transaction configuration used by a {@link Transaction}. In the beginning this was all
 * placed in the Transaction, adding a lot of 'informational' methods to the transaction and therefor
 * complicating its usage. So all the configurational properties of the transaction are contained in
 * this structure.
 * <p/>
 * The same TransactionConfiguration is used for multiple transactions. Each TransactionFactory has just a
 * single configuration and all Transactions created by that TransactionFactory, share that configuration.
 *
 * @author Peter Veentjer.
 */
public interface TransactionConfiguration {

    /**
     * Returns the total timeout in nanoseconds. Long.MAX_VALUE indicates that there is no
     * timeout.
     *
     * @return the total remaining timeout.
     * @see TransactionFactoryBuilder#setTimeoutNs(long)
     */
    long getTimeoutNs();

    /**
     * Returns the PropagationLevel used. With the PropagationLevel you have control on how the transaction
     * is dealing with nesting of transactions.
     *
     * @return the PropagationLevel used.
     * @see org.multiverse.api.TransactionFactoryBuilder#setPropagationLevel(PropagationLevel)
     */
    PropagationLevel getPropagationLevel();

    /**
     * Returns the TraceLevel. With the TraceLevel you have control on the logging.
     *
     * @return the TraceLevel.
     * @see org.multiverse.api.TransactionFactoryBuilder#setTraceLevel(TraceLevel)
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
     * Checks if speculative configuration is enabled. When enabled the STM is able to select better
     * performing/scalable implementations at the cost of some
     * {@link org.multiverse.api.exceptions.SpeculativeConfigurationError}.
     *
     * @return true if speculative configuration is enabled.
     * @see TransactionFactoryBuilder#setSpeculativeConfigurationEnabled(boolean)
     */
    boolean isSpeculativeConfigEnabled();

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
     * Checks if this Transaction is readonly. With a readonly transaction you can prevent any updates or
     * new objects being created.
     *
     * @return true if readonly, false otherwise.
     * @see TransactionFactoryBuilder#setReadonly(boolean)
     */
    boolean isReadonly();

    /**
     * Returns the maximum number of times the transaction is allowed to spin on a read to become
     * readable (perhaps it is locked).
     *
     * @return the maximum number of spins
     * @see org.multiverse.api.TransactionFactoryBuilder#setSpinCount(int)
     */
    int getSpinCount();

    /**
     * Returns the PessimisticLockLevel used on all reads/writes.
     *
     * @return the PessimisticLockLevel.
     * @see org.multiverse.api.TransactionFactoryBuilder#setPessimisticLockLevel(PessimisticLockLevel)
     */
    PessimisticLockLevel getPessimisticLockLevel();

    /**
     * Checks if dirty check is enabled on writes when a transaction commits. Turning of saves time,
     * but forces writes that cause no change.
     *
     * @return true of dirty check is enabled.
     * @see org.multiverse.api.TransactionFactoryBuilder#setDirtyCheckEnabled(boolean)
     */
    boolean isDirtyCheck();

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
     * Returns the Stm that creates transaction based on this configuration.
     *
     * @return the stm.
     */
    Stm getStm();

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
     * If an explicit retry (so a blocking transaction) is allowed. With this property one can prevent
     * that a Transaction is able to block waiting for some change.
     *
     * @return true if explicit retry is allowed, false otherwise.
     * @see TransactionFactoryBuilder#setBlockingAllowed(boolean) (boolean)
     */
    boolean isBlockingAllowed();

    /**
     * Checks if the Transaction can be interrupted if it is blocking.
     *
     * @return true if the Transaction can be interrupted if it is blocking, false otherwise.
     * @see TransactionFactoryBuilder#setInterruptible(boolean)
     */
    boolean isInterruptible();

    /**
     * Returns an unmodifiable list containing all permanent TransactionLifecycleListeners.
     *
     * @return unmodifiable List containing all permanent TransactionLifecycleListeners.
     */
    List<TransactionLifecycleListener> getPermanentListeners();

    /**
     * Returns the maximum number of times this Transaction be retried before failing. The returned value will
     * always be equal or larger than 0. If the value is getAndSet high and you are encountering a lot of
     * TooManyRetryExceptions it could be that the objects are just not concurrent enough.
     *
     * @return the maxRetries.
     * @see TransactionFactoryBuilder#setMaxRetries(int)
     */
    int getMaxRetries();
}
