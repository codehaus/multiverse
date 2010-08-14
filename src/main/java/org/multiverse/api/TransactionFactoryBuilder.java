package org.multiverse.api;

/**
 * @author Peter Veentjer
 */
public interface TransactionFactoryBuilder {

    TransactionFactoryBuilder setPropagationLevel(PropagationLevel propagationLevel);

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
     */
    AtomicBlock buildAtomicBlock();
}
