package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.*;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.gamma.GammaAtomicBlock;

public interface GammaTransactionFactoryBuilder extends TransactionFactoryBuilder{

    @Override
    GammaTransactionConfiguration getTransactionConfiguration();

    @Override
    GammaTransactionFactoryBuilder setReadLockLevel(LockMode lockMode);

    @Override
    GammaTransactionFactoryBuilder setWriteLockLevel(LockMode lockMode);

    @Override
    GammaTransactionFactoryBuilder setFamilyName(String familyName);

    @Override
    GammaTransactionFactoryBuilder setPropagationLevel(PropagationLevel propagationLevel);

    @Override
    GammaTransactionFactoryBuilder setLockLevel(LockLevel lockLevel);

    @Override
    GammaTransactionFactoryBuilder addPermanentListener(TransactionLifecycleListener listener);

    @Override
    GammaTransactionFactoryBuilder setTraceLevel(TraceLevel traceLevel);

    @Override
    GammaTransactionFactoryBuilder setTimeoutNs(long timeoutNs);

    @Override
    GammaTransactionFactoryBuilder setInterruptible(boolean interruptible);

    @Override
    GammaTransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy);

    @Override
    GammaTransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled);

    @Override
    GammaTransactionFactoryBuilder setSpinCount(int spinCount);

    @Override
    GammaTransactionFactoryBuilder setReadonly(boolean readonly);

    @Override
    GammaTransactionFactoryBuilder setReadTrackingEnabled(boolean enabled);

    @Override
    GammaTransactionFactoryBuilder setSpeculativeConfigurationEnabled(boolean enabled);

    @Override
    GammaTransactionFactoryBuilder setMaxRetries(int maxRetries);

    @Override
    GammaTransactionFactoryBuilder setIsolationLevel(IsolationLevel isolationLevel);

    @Override
    GammaTransactionFactoryBuilder setBlockingAllowed(boolean blockingAllowed);

    @Override
    GammaTransactionFactory build();

    @Override
    GammaAtomicBlock buildAtomicBlock();
}
