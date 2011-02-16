package org.multiverse.api;

import org.multiverse.api.lifecycle.TransactionListener;

/**
 * A Builder for creating a {@link TransactionFactory}. This builder provides full control on transaction
 * settings.
 *
 * <p>Instances of this class are considered immutable, so when you call one of the modifying methods, make sure
 * that you use the resulting TransactionFactoryBuilder. Normally with the builder implementation the same
 * instance is returned. In this case this isn't true because a new instance is returned every time.
 *
 * @author Peter Veentjer
 * @see TransactionFactory
 * @see TransactionConfiguration
 */
public interface TransactionFactoryBuilder {

    /**
     * Returns the TransactionConfiguration used by this TransactionFactoryBuilder.
     *
     * @return the used TransactionConfiguration.
     */
    TransactionConfiguration getConfiguration();

    /**
     * Sets if ControlFlowErrors are reused. Normally you don't want to reuse them because they can be expensive
     * to create (especially the stacktrace) and they could be created very often. But for debugging purposes it
     * can be quite annoying because you want to see the stacktrace.
     *
     * @param reused true if ControlFlowErrors should be reused.
     * @return the updated TransactionFactoryBuilder.
     * @see TransactionConfiguration#isControlFlowErrorsReused()
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
     * @see TransactionConfiguration#getFamilyName()
     */
    TransactionFactoryBuilder setFamilyName(String familyName);

    /**
     * Sets the {@link org.multiverse.api.PropagationLevel} used. With the PropagationLevel you have control
     * on how the transaction deals with transaction nesting.
     *
     * @param propagationLevel the new PropagationLevel
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if propagationLevel is null.
     * @see TransactionConfiguration#getPropagationLevel()
     * @see PropagationLevel
     */
    TransactionFactoryBuilder setPropagationLevel(PropagationLevel propagationLevel);

    /**
     * Sets the LockMode for all reads.
     *
     * @param lockMode the LockMode to set.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if lockMode is null.
     * @see TransactionConfiguration#getReadLockMode()
     * @see LockMode
     */
    TransactionFactoryBuilder setReadLockMode(LockMode lockMode);

    /**
     * Sets the LockMode for all writes. For a write, always a read needs to be done, so if the ReadLockMode is
     * <p/>
     * Freshly constructed objects that are not committed, automatically are locked with an Exclusive lock.
     * <p/>
     * If the write LockMode is set after the read LockMode and the write LockMode is lower than the read LockMode,
     * a IllegalTransactionFactoryException will be thrown when a TransactionFactory is created.
     * <p/>
     * If the write LockMode is set before the read LockMode and the write LockMode is lower than the read LockMode,
     * the write LockMode automatically is upgraded to that of the read LockMode. This makes setting the readLock
     * mode less of a nuisance.
     *
     * @param lockMode the LockMode to set.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if lockMode is null.
     * @see TransactionConfiguration#getWriteLockMode()
     * @see LockMode
     */
    TransactionFactoryBuilder setWriteLockMode(LockMode lockMode);

    /**
     * Adds a permanent Listener to this TransactionFactoryBuilder. All permanent listeners are always executed
     * after all normal listeners are executed. If the same listener is added multiple times, it will be executed
     * multiple times.
     * <p/>
     * This method is very useful for integrating Multiverse in other JVM based environments because with this
     * approach you have a callback when transaction aborts/commit and can add your own logic.
     *
     * @param listener the permanent listener to add.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if listener is null.
     * @see TransactionConfiguration#getPermanentListeners()
     */
    TransactionFactoryBuilder addPermanentListener(TransactionListener listener);

    /**
     * Sets the TraceLevel. With tracing it is possible to see what is happening inside a transaction.
     *
     * @param traceLevel the new traceLevel.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if traceLevel is null.
     * @see TransactionConfiguration#getTraceLevel()
     * @see TraceLevel
     */
    TransactionFactoryBuilder setTraceLevel(TraceLevel traceLevel);

    /**
     * Sets the timeout (the maximum time a transaction is allowed to block. Long.MAX_VALUE indicates that an
     * unbound timeout should be used.
     *
     * @param timeoutNs the timeout specified in nano seconds
     * @return the updated TransactionFactoryBuilder
     * @see TransactionConfiguration#getTimeoutNs()
     * @see Transaction#getRemainingTimeoutNs()
     */
    TransactionFactoryBuilder setTimeoutNs(long timeoutNs);

    /**
     * Sets if the transaction can be interrupted while doing blocking operations.
     *
     * @param interruptible if the transaction can be interrupted while doing blocking operations.
     * @return the updated TransactionFactoryBuilder
     * @see TransactionConfiguration#isInterruptible()
     */
    TransactionFactoryBuilder setInterruptible(boolean interruptible);

    /**
     * Sets the new backoff policy. Policy is used to backoff when a transaction conflicts with another transaction.
     * See the {@link BackoffPolicy} for more information.
     *
     * @param backoffPolicy the backoff policy to use.
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if backoffPolicy is null.
     * @see TransactionConfiguration#getBackoffPolicy()
     */
    TransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy);

    /**
     * Sets if the dirty check is enabled. Dirty check is that something only needs to be written, if there really
     * is a change. If it is disabled, it will always write, and this could prevent the aba isolation anomaly, but
     * causes more conflicts so more contention. In most cases enabling it is the best option.
     *
     * @param dirtyCheckEnabled true if dirty check should be executed, false otherwise.
     * @return the updated TransactionFactoryBuilder.
     * @see TransactionConfiguration#isDirtyCheckEnabled()
     */
    TransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled);

    /**
     * Returns the maximum number of spins that should be executed when a transactional object can't be read
     * because it is locked.
     *
     * @param spinCount the maximum number of spins
     * @return the updated TransactionFactoryBuilder.
     * @throws IllegalArgumentException if spinCount smaller than 0.
     * @see TransactionConfiguration#getSpinCount()
     */
    TransactionFactoryBuilder setSpinCount(int spinCount);

    /**
     * Sets the readonly property on a transaction. Readonly transactions are always cheaper than update transactions.
     * <p/>
     * If this property is set, the stm will not speculate on this property anymore.
     *
     * @param readonly true if the transaction should be readonly, false otherwise.
     * @return the updated TransactionFactoryBuilder
     * @see TransactionConfiguration#isReadonly()
     */
    TransactionFactoryBuilder setReadonly(boolean readonly);

    /**
     * Sets if the transaction should automatically track all reads that have been done. This is needed for blocking
     * operations, but also for other features like writeskew detection.
     * <p/>
     * If this property is set, the stm will not speculate on this property anymore.
     * <p/>
     * The transaction is free to track reads even though this property is disabled.
     *
     * @param enabled true if read tracking enabled, false otherwise.
     * @return the updated TransactionFactoryBuilder
     * @see TransactionConfiguration#isReadTrackingEnabled()
     */
    TransactionFactoryBuilder setReadTrackingEnabled(boolean enabled);

    /**
     * With the speculative configuration enabled, the stm is allowed to determine optimal settings for transactions.
     * Some behavior like readonly or the need for tracking reads can be determined runtime. The system can newTransaction with
     * a readonly non readtracking transaction and upgrade to an update or a read tracking once a write or retry
     * happens.
     *
     * @param speculative indicates if speculative configuration should be enabled.
     * @return the updated TransactionFactoryBuilder
     * @see TransactionConfiguration#isSpeculative()
     */
    TransactionFactoryBuilder setSpeculative(boolean speculative);

    /**
     * Sets the the maximum count a transaction can be retried. The default is 1000. Setting it to a very low value
     * could mean that a transaction can't complete. Setting it to a very high value could lead to live-locking.
     *
     * @param maxRetries the maximum number of times a transaction can be tried.
     * @return the updated TransactionFactoryBuilder
     * @throws IllegalArgumentException if maxRetries smaller than 0.
     * @see TransactionConfiguration#getMaxRetries()
     */
    TransactionFactoryBuilder setMaxRetries(int maxRetries);

    /**
     * Sets the isolation level.
     * <p/>
     * The transaction is free to upgraded to a higher isolation level.
     *
     * @param isolationLevel the new IsolationLevel
     * @return the updated TransactionFactoryBuilder
     * @throws NullPointerException if isolationLevel is null.
     * @see TransactionConfiguration#getIsolationLevel()
     * @see IsolationLevel
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
     * Builds an {@link AtomicBlock} optimized for executing transactions created by this TransactionFactoryBuilder.
     *
     * @return the created AtomicBlock.
     * @throws org.multiverse.api.exceptions.IllegalTransactionFactoryException
     *          if the TransactionFactory could not be build
     *          because the configuration was not correct.
     */
    AtomicBlock newAtomicBlock();
}
