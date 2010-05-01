package org.multiverse.api;

import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.commitlock.CommitLockPolicy;

/**
 * An implementation of the builder design pattern to createReference a {@link TransactionFactory}. This is the place
 * to be for transaction configuration. This approach also gives the freedom to access implementation specific
 * setters by implementing and extending the {@link TransactionFactoryBuilder} interface.
 * <p/>
 * <h2>Usage</h2>
 * In most cases the TransactionFactoryBuilder will be dropped once the TransactionFactory
 * is created (the TransactionFactory is the one you want to store for later use).
 * <p/>
 * <h2>Chained methods</h2>
 * It is important that the returned TransactionFactoryBuilder is used on the 'set' method because TransactionBuilders
 * are immutable. If you don't do this, your transactions don't get the properties you want them to have.
 * <p/>
 * <h2>Transaction familyName</h2>
 * This is purely used for debugging/profiling purposes. In the instrumentation the familyname is determined by the
 * full method signature including the owning class.
 * <p/>
 * The big advantage to a builder compared to just adding a big load of parameters or storing these parameters in a
 * datastructure (so object without logic) is that the Stm implementation has a lot more room for adding custom
 * parameters and stm-internal transaction-family related dependencies.
 * <p/>
 * TransactionBuilders are immutable and therefor thread safe to use.
 *
 * @author Peter Veentjer.
 */
public interface TransactionFactoryBuilder<T extends Transaction, B extends TransactionFactoryBuilder> {

    /**
     * Checks if dirty check is enabled.
     *
     * @return true if enabled, false otherwise.
     * @see #setDirtyCheckEnabled(boolean)
     */
    boolean isDirtyCheckEnabled();

    /**
     * Sets id the dirty check is enabled. Dirty check is that something only needs to be written, if there really
     * is a change. If it is disabled, it will always write, and this could prevent the aba isolation anomaly, but
     * causes more conflicts so more contention. In most cases enabling it is the best option.
     *
     * @param dirtyCheckEnabled true if dirty check should be executed, false otherwise.
     * @return the updated TransactionFactoryBuilder.
     * @see #isDirtyCheckEnabled()
     */
    B setDirtyCheckEnabled(boolean dirtyCheckEnabled);

    /**
     * Sets if the Transaction is allowed to do an explicit retry (needed for a blocking operation). One use case
     * for disallowing it, it when the transaction is used inside an agent, and you don't want that inside the logic
     * executed by the agent a blocking operations is done (e.g. taking an item of a blocking queue).
     *
     * @param explicitRetryEnabled true if explicit retry is enabled, false otherwise.
     * @return the updated TransactionFactoryBuilder
     * @see #isExplicitRetryAllowed()
     */
    B setExplicitRetryAllowed(boolean explicitRetryEnabled);

    /**
     * Checks if explicit retry (for blocking operations) is allowed.
     *
     * @return true if allowed, false otherwise.
     * @see #setExplicitRetryAllowed(boolean)
     */
    boolean isExplicitRetryAllowed();

    /**
     * Sets the transaction familyname.
     * <p/>
     * The transaction familyName is useful debugging purposes. With Multiverse 0.4 it was
     * also needed for speculative configuration, but that requirement is dropped.
     *
     * @param familyName the familyName of the transaction.
     * @return the updated TransactionFactoryBuilder
     * @see #getFamilyName()
     */
    B setFamilyName(String familyName);

    /**
     * Returns the family name of the transaction.
     *
     * @return the familyname of the transaction.
     * @see #setFamilyName(String)
     */
    String getFamilyName();

    /**
     * Sets the readonly property on a transaction. Readonly transactions are always cheaper than update transactions.
     * <p/>
     * If this property is set, the stm will not speculate on this property anymore.
     *
     * @param readonly true if the transaction should be readonly, false otherwise.
     * @return the updated TransactionFactoryBuilder
     * @see #isReadonly()
     */
    B setReadonly(boolean readonly);

    /**
     * Checks if the transaction is readonly or an update.
     *
     * @return true if enabled, false otherwise.
     * @see #setReadonly(boolean)
     */
    boolean isReadonly();

    /**
     * Sets if the transaction should automatically track all reads that have been done. This is needed for blocking
     * operations, but also for other features like writeskew detection.
     * <p/>
     * If this property is set, the stm will not speculate on this property anymore.
     *
     * @param enabled true if read tracking enabled, false otherwise.
     * @return the updated TransactionFactoryBuilder
     * @see #isQuickReleaseEnabled()
     */
    B setReadTrackingEnabled(boolean enabled);

    /**
     * Checks if the transaction automatically tracks reads.
     *
     * @return true if automatic read tracking is enabled, false otherwise.
     * @see #setReadTrackingEnabled(boolean)
     */
    boolean isReadTrackingEnabled();

    /**
     * Sets if the transaction can be interrupted while doing blocking operations.
     *
     * @param interruptible if the transaction can be interrupted while doing blocking operations.
     * @return the updated TransactionFactoryBuilder
     * @see #isInterruptible()
     */
    B setInterruptible(boolean interruptible);

    /**
     * Checks if the transaction is interruptible.
     *
     * @return true if interruptible, false otherwise.
     * @see #setInterruptible(boolean)
     */
    boolean isInterruptible();

    /**
     * Sets the CommitLockPolicy. The CommitLockPolicy is only used by update transactions that commit: when the stm
     * commits, locks need to be acquired on dirty objects.
     *
     * @param commitLockPolicy the new CommitLockPolicy
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if commitLockPolicy is null.
     * @see #getCommitLockPolicy()
     */
    B setCommitLockPolicy(CommitLockPolicy commitLockPolicy);

    /**
     * Returns the CommitLockPolicy
     *
     * @return the CommitLockPolicy.
     */
    CommitLockPolicy getCommitLockPolicy();

    /**
     * With the speculative configuration enabled, the stm is allowed to determine optimal settings for transactions.
     * Some behavior like readonly or the need for tracking reads can be determined runtime. The system can start with
     * a readonly non readtracking transaction and upgrade to an update or a read tracking once a write or retry
     * happens.
     *
     * @param newValue indicates if speculative configuration should be enabled.
     * @return the updated TransactionFactoryBuilder
     * @see #isSpeculativeConfigurationEnabled()
     */
    B setSpeculativeConfigurationEnabled(boolean newValue);

    /**
     * Checks if speculative configuration is enabled.
     *
     * @return true if enabled, false otherwise.
     * @see #setSpeculativeConfigurationEnabled(boolean)
     */
    boolean isSpeculativeConfigurationEnabled();

    /**
     * If writeskew problem is allowed to happen. Defaults to true and can have a big impact on performance (the
     * complete read set needs to be validated and not just the writes). So disallow it wisely.
     *
     * @param allowWriteSkew indicates if writeSkew problem is allowed.
     * @return the updated TransactionFactoryBuilder
     * @see #isWriteSkewAllowed()
     */
    B setWriteSkewAllowed(boolean allowWriteSkew);

    /**
     * Checks if the writeskew problem is allowed to happen.
     *
     * @return true if allowed, false otherwise.
     * @see #setWriteSkewAllowed(boolean)
     */
    boolean isWriteSkewAllowed();

    /**
     * Sets enabling quick release on locks is enabled. When a transaction commits, it needs to acquire the writelocks.
     * If quick release is disabled, first all writes are executed before any lock is released. With quick release
     * enabled, the lock on the transactional object is released as soon as the write is done.
     * <p/>
     * The 'disadvantage' of having this enabled is that it could happen that some objects modified in a transaction
     * are releases and some are not. If another transaction picks up these objects, it could be that it is able to
     * read some and fails on other. Normally this isn't an issue because the transaction is retried in combination
     * with a back off policy.
     *
     * @param enabled true if the lock of a transaction object should be releases as soon as possible instead
     *                of waiting for the whole transaction to commit.
     * @return the updated TransactionFactoryBuilder
     * @see #isQuickReleaseEnabled()
     */
    B setQuickReleaseEnabled(boolean enabled);

    /**
     * Checks if quick release of locks while committing is enabled.
     *
     * @return true if enabled, false otherwise.
     * @see #setQuickReleaseEnabled(boolean)
     */
    boolean isQuickReleaseEnabled();

    /**
     * Sets the new backoff policy. Policy is used to backoff when a transaction conflicts with another transaction.
     * See the {@link BackoffPolicy} for more information.
     *
     * @param backoffPolicy the backoff policy to use.
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if backoffPolicy is null.
     * @see #getBackoffPolicy()
     */
    B setBackoffPolicy(BackoffPolicy backoffPolicy);

    /**
     * Return the BackoffPolicy used.
     *
     * @return the BackoffPolicy.
     * @see #setBackoffPolicy(org.multiverse.api.backoff.BackoffPolicy)
     */
    BackoffPolicy getBackoffPolicy();

    /**
     * Sets the timeout (the maximum time a transaction is allowed to block. Long.MAX_VALUE indicates that no timeout
     * should be used.
     *
     * @param timeoutNs the timeout specified in nano seconds
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if unit is null.
     */
    B setTimeoutNs(long timeoutNs);

    /**
     * Returns the timeout. Value will always be equal or larger than zero.
     *
     * @return the timeout.
     * @see #setTimeoutNs(long)
     */
    long getTimeoutNs();

    /**
     * Sets the the maximum count a transaction can be retried. The default is 1000. Setting it to a very low value
     * could mean that a transaction can't complete.
     *
     * @param maxRetries the maximum number of times a transaction can be tried.
     * @return the updated TransactionFactoryBuilder
     * @throws IllegalArgumentException if retryCount smaller than 0.
     */
    B setMaxRetries(int maxRetries);

    /**
     * Returns the maximum number of times a transaction can be retried. It doesn't matter if a retry is caused by
     * explicit retries, or by more accidental retries(read/write conflicts).
     *
     * @return the maximum number of times a transaction can be retried. The returned
     *         value will always be equal or larger than 0.
     */
    int getMaxRetries();

    /**
     * Sets the maximum number of spins that should be executed when a transactional object can't be read because it
     * is locked. Watch out with setting this value too high, because a system could start livelocking.
     *
     * @param maxReadSpinCount the maximum number of spins.
     * @return the updated TransactionFactoryBuilder.
     * @throws IllegalArgumentException if smaller than 0.
     * @see #getMaxReadSpinCount()
     */
    B setMaxReadSpinCount(int maxReadSpinCount);

    /**
     * Returns the maximum number of spins that should be executed when a transactional object can't be read because
     * it is locked. Value will always be equal or larger than 0.
     *
     * @return the maximum number of spins
     * @see #setMaxReadSpinCount(int)
     */
    int getMaxReadSpinCount();

    // void setLoggingOfControlFlowErrorsEnabled(boolean enabled);

    // boolean isLoggingOfControlFlowErrorsEnabled();

    /**
     * Builds a {@link TransactionFactory} with the provided configuration.
     *
     * @return the started Transaction.
     * @throws IllegalStateException if this TransactionFactoryBuilder is not configured correctly and therefor the
     *                               TransactionFactory can't be created.
     */
    TransactionFactory<T> build();
}
