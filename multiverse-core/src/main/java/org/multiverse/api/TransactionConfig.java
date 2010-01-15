package org.multiverse.api;

import org.multiverse.utils.backoff.BackoffPolicy;

/**
 * Contains the transaction configuration used by a {@link Transaction}. In the beginning this was all placed in the
 * Transaction, adding a lot of 'informational' methods to the transaction and therefor complicating its usage. So
 * all the configurational stuff was moved to a specialized object: the TransactionConfig.
 *
 * @author Peter Veentjer.
 */
public interface TransactionConfig {

    /**
     * Returns the BackoffPolicy used.
     *
     * @return the BackoffPolicy used.
     */
    BackoffPolicy getRetryBackoffPolicy();

    /**
     * Checks if this transaction does automaticReadTracking.
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
     * always be equal or larger than 0.
     *
     * @return the maxRetryCount.
     */
    int getMaxRetryCount();

    /**
     * Checks if the Transaction can be interrupted if it is waiting.
     *
     * @return true if the Transaction can be interrupted if it is waiting, false otherwise.
     */
    boolean isInterruptible();

    /**
     * Checks if this Transaction is readonly.
     *
     * @return true if readonly, false otherwise.
     */
    boolean isReadonly();

    /**
     * Checks if this Transaction should prent writeskew.
     * <p/>
     * todo: explanation about writeskew
     * <p/>
     * If the transaction is readonly, the value is true since it won't suffer from
     * write skew problems.
     *
     * @return true if a writeskews are prevented, false otherwise.
     */
    boolean preventWriteSkew();
}
