package org.multiverse.api;

import org.multiverse.api.lifecycle.TransactionLifecycleListener;

/**
 * A Builder for creating a {@link TransactionFactory}. This builder provides full control on transaction
 * settings.
 * <p/>
 * Instances of this class are considered immutable, so when you call one of the modifying methods, make sure
 * that you use the resulting TransactionFactoryBuilder. Normally with the builder implementation the same
 * instance is returned. In this case this isn't true because a new instance is returned every time.
 *
 * @author Peter Veentjer
 */
public interface TransactionFactoryBuilder {

    /**
     * Returns the TransactionConfiguration used by this TransactionFactoryBuilder.
     *
     * @return the used TransactionConfiguration.
     */
    TransactionConfiguration getTransactionConfiguration();

    /**
     * Sets if ControlFlowErrors are reused. Normally you don't want to reuse them because they can be expensive
     * to create (especially the stacktrace) and they could be created very often. But for debugging purposes it
     * can be quite annoying.
     *
     * @param reused true if ControlFlowErrors should be reused.
     * @return the updated TransactionFactoryBuilder.
     */
    TransactionFactoryBuilder setControlFlowErrorsReused(boolean reused);

    /**
     * Sets the transaction familyname.
     * <p/>
     * The transaction familyName is useful debugging purposes. With Multiverse 0.4 it was
     * also needed for speculative configuration, but that requirement is dropped.
     *
     * @param familyName the familyName of the transaction.
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if familyName is null.
     */
    TransactionFactoryBuilder setFamilyName(String familyName);

    /**
     * Sets the {@link org.multiverse.api.PropagationLevel} used. With the PropagationLevel you have control
     * on how the transaction deals with transaction nesting.
     *
     * @param propagationLevel the new PropagationLevel
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if propagationLevel is null.
     */
    TransactionFactoryBuilder setPropagationLevel(PropagationLevel propagationLevel);

    /**
     * Sets the LockMode for all reads.
     *
     * @param lockMode the LockMode to set.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if lockMode is null.
     */
    TransactionFactoryBuilder setReadLockMode(LockMode lockMode);

    /**
     * Sets the LockMode for all writes. For a write, always a read needs to be done, so if the ReadLockMode is
     * <p/>
     * Freshly constructed objects that are not committed, automatically are locked with an Exclusive lock.
     *
     * @param lockMode the LockMode to set.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if lockMode is null.
     */
    TransactionFactoryBuilder setWriteLockMode(LockMode lockMode);

    /**
     * Sets the {@link LockLevel}. With the LockLevel you have control
     * on transaction level on how pessimistic or optimistic a transaction is.
     *
     * @param lockLevel the new LockLevel
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if lockLevel is null.
     */
    @Deprecated
    TransactionFactoryBuilder setLockLevel(LockLevel lockLevel);

    /**
     * Adds a permanent Listener to this TransactionFactoryBuilder. All permanent listeners are always executed
     * after all normal listeners are executed. If the same listener is added multiple times, it will be executed
     * multiple times.
     * <p/>
     * This method is very useful for integrating Multiverse in other JVM based environments because with this
     * approach you have a callback when transaction newTransaction/___abort/commit and can add your own logic.
     *
     * @param listener the permanent listener to add.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if listener is null.
     */
    TransactionFactoryBuilder addPermanentListener(TransactionLifecycleListener listener);

    /**
     * Sets the TraceLevel. With tracing it is possible to see what is happening inside a transaction.
     *
     * @param traceLevel the new traceLevel.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if traceLevel is null.
     * @see TransactionConfiguration#getTraceLevel()
     */
    TransactionFactoryBuilder setTraceLevel(TraceLevel traceLevel);

    /**
     * Sets the timeout (the maximum time a transaction is allowed to block. Long.MAX_VALUE indicates that an
     * unbound timeout should be used.
     *
     * @param timeoutNs the timeout specified in nano seconds
     * @return the updated TransactionFactoryBuilder
     */
    TransactionFactoryBuilder setTimeoutNs(long timeoutNs);

    /**
     * Sets if the transaction can be interrupted while doing blocking operations.
     *
     * @param interruptible if the transaction can be interrupted while doing blocking operations.
     * @return the updated TransactionFactoryBuilder
     */
    TransactionFactoryBuilder setInterruptible(boolean interruptible);

    /**
     * Sets the new backoff policy. Policy is used to backoff when a transaction conflicts with another transaction.
     * See the {@link BackoffPolicy} for more information.
     *
     * @param backoffPolicy the backoff policy to use.
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if backoffPolicy is null.
     */
    TransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy);

    /**
     * Sets if the dirty check is enabled. Dirty check is that something only needs to be written, if there really
     * is a change. If it is disabled, it will always write, and this could prevent the aba isolation anomaly, but
     * causes more conflicts so more contention. In most cases enabling it is the best option.
     *
     * @param dirtyCheckEnabled true if dirty check should be executed, false otherwise.
     * @return the updated TransactionFactoryBuilder.
     */
    TransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled);

    /**
     * Returns the maximum number of spins that should be executed when a transactional object can't be read
     * because it is locked.
     *
     * @param spinCount the maximum number of spinds
     * @return the updated TransactionFactoryBuilder.
     * @throws IllegalArgumentException if spinCount smaller than 0.
     */
    TransactionFactoryBuilder setSpinCount(int spinCount);

    /**
     * Sets the readonly property on a transaction. Readonly transactions are always cheaper than update transactions.
     * <p/>
     * If this property is getAndSet, the stm will not speculate on this property anymore.
     *
     * @param readonly true if the transaction should be readonly, false otherwise.
     * @return the updated TransactionFactoryBuilder
     */
    TransactionFactoryBuilder setReadonly(boolean readonly);

    /**
     * Sets if the transaction should automatically track all reads that have been done. This is needed for blocking
     * operations, but also for other features like writeskew detection.
     * <p/>
     * If this property is getAndSet, the stm will not speculate on this property anymore.
     *
     * @param enabled true if read tracking enabled, false otherwise.
     * @return the updated TransactionFactoryBuilder
     */
    TransactionFactoryBuilder setReadTrackingEnabled(boolean enabled);

    /**
     * With the speculative configuration enabled, the stm is allowed to determine optimal settings for transactions.
     * Some behavior like readonly or the need for tracking reads can be determined runtime. The system can newTransaction with
     * a readonly non readtracking transaction and upgrade to an update or a read tracking once a write or retry
     * happens.
     *
     * @param enabled indicates if speculative configuration should be enabled.
     * @return the updated TransactionFactoryBuilder
     */
    TransactionFactoryBuilder setSpeculativeConfigurationEnabled(boolean enabled);

    /**
     * Sets the the maximum count a transaction can be retried. The default is 1000. Setting it to a very low value
     * could mean that a transaction can't complete. Setting it to a very high value could lead to livelocking.
     *
     * @param maxRetries the maximum number of times a transaction can be tried.
     * @return the updated TransactionFactoryBuilder
     * @throws IllegalArgumentException if maxRetries smaller than 0.
     */
    TransactionFactoryBuilder setMaxRetries(int maxRetries);

    /**
     * Sets the isolation level.
     *
     * @param isolationLevel the new IsolationLevel
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if isolationLevel is null.
     */
    TransactionFactoryBuilder setIsolationLevel(IsolationLevel isolationLevel);

    /**
     * Sets if the Transaction is allowed to do an explicit retry (needed for a blocking operation). One use case
     * for disallowing it, it when the transaction is used inside an actor, and you don't want that inside the logic
     * executed by the agent a blocking operations is done (e.g. taking an item of a blocking queue).
     *
     * @param blockingAllowed true if explicit retry is allowed, false otherwise.
     * @return the updated TransactionFactoryBuilder
     */

    TransactionFactoryBuilder setBlockingAllowed(boolean blockingAllowed);

    /**
     * Builds a new {@link TransactionFactory}.
     *
     * @return the build TransactionFactory.
     * @throws org.multiverse.api.exceptions.IllegalTransactionFactoryException
     *          if the TransactionFactory could not be build
     *          because the configuration was not correct.
     */
    TransactionFactory newTransactionFactory();

    /**
     * Builds an AtomicBlock optimized for a transactions created by this TransactionFactoryBuilder.
     *
     * @return the created AtomicBlock.
     * @throws org.multiverse.api.exceptions.IllegalTransactionFactoryException
     *          if the TransactionFactory could not be build
     *          because the configuration was not correct.
     */
    AtomicBlock newAtomicBlock();
}
