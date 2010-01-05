package org.multiverse.api;

import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

public interface TransactionConfig {

    RestartBackoffPolicy getRestartBackoffPolicy();

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
     * Checks if this Transaction should detect writeskew.
     * <p/>
     * todo: explanation about writeskew
     * <p/>
     * If the transaction is readonly, the value is not specified.
     * If the transaction
     *
     * @return true if a writeskews is detected, false otherwise.
     */
    boolean detectWriteSkew();
}
