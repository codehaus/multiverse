package org.multiverse.api;

/**
 * @author Peter Veentjer
 */
public interface TransactionFactoryBuilder {

    /**
     * Returns the TransactionConfiguration used by this TransactionFactoryBuilder.
     *
     * @return
     */
    TransactionConfiguration getTransactionConfiguration();

    TransactionFactoryBuilder setFamilyName(String familyName);

    TransactionFactoryBuilder setPropagationLevel(PropagationLevel propagationLevel);

    /**
     * Sets the TraceLevel. With tracing it is possible to see what is happening inside a transaction.
     *
     * @param traceLevel the new traceLevel.
     * @return the updated TransactionFactoryBuilder.
     * @throws NullPointerException if traceLevel is null.
     * @see TransactionConfiguration#getTraceLevel()
     */
    TransactionFactoryBuilder setTraceLevel(TraceLevel traceLevel);

    TransactionFactoryBuilder setTimeoutNs(long timeoutNs);

    TransactionFactoryBuilder setInterruptible(boolean interruptible);

    TransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy);

    TransactionFactoryBuilder setPessimisticLockLevel(PessimisticLockLevel lockLevel);

    TransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled);

    TransactionFactoryBuilder setSpinCount(int spinCount);

    TransactionFactoryBuilder setReadonly(boolean readonly);

    TransactionFactoryBuilder setReadTrackingEnabled(boolean enabled);

    TransactionFactoryBuilder setSpeculativeConfigEnabled(boolean speculativeConfigEnabled);

    TransactionFactoryBuilder setMaxRetries(int maxRetries);

    TransactionFactoryBuilder setWriteSkewAllowed(boolean writeSkewAllowed);

    TransactionFactoryBuilder setBlockingAllowed(boolean blockingEnabled);

    /**
     * Builds a new {@link TransactionFactory}.
     *
     * @return the build TransactionFactory.
     * @throws org.multiverse.api.exceptions.IllegalTransactionFactoryException
     *          if the TransactionFactory could not be build
     *          because the configuration was not correct.
     */
    TransactionFactory build();

    /**
     * Builds an AtomicBlock optimized for a transactions created by this TransactionFactoryBuilder.
     *
     * @return the created AtomicBlock.
     * @throws org.multiverse.api.exceptions.IllegalTransactionFactoryException
     *          if the TransactionFactory could not be build
     *          because the configuration was not correct.
     */
    AtomicBlock buildAtomicBlock();
}
