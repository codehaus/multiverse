package org.multiverse.stms.beta;

import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

/**
 * A {@link org.multiverse.api.TransactionFactoryBuilder}  for the {@link org.multiverse.stms.beta.BetaStm}.
 *
 * @author Peter Veentjer.
 */
public interface BetaTransactionFactoryBuilder extends TransactionFactoryBuilder {

    @Override
    BetaTransactionFactoryBuilder setControlFlowErrorsReused(boolean reused);

    @Override
    BetaTransactionConfiguration getTransactionConfiguration();

    @Override
    BetaTransactionFactoryBuilder addPermanentListener(TransactionLifecycleListener listener);

    @Override
    BetaTransactionFactoryBuilder setFamilyName(String familyName);

    @Override
    BetaTransactionFactoryBuilder setPropagationLevel(PropagationLevel level);

    @Override
    BetaTransactionFactoryBuilder setBlockingAllowed(boolean blockingAllowed);

    @Override
    BetaTransactionFactoryBuilder setIsolationLevel(IsolationLevel isolationLevel);

    @Override
    BetaTransactionFactoryBuilder setTraceLevel(TraceLevel traceLevel);

    @Override
    BetaTransactionFactoryBuilder setTimeoutNs(long timeoutNs);

    @Override
    BetaTransactionFactoryBuilder setInterruptible(boolean interruptible);

    @Override
    BetaTransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy);

    @Override
    BetaTransactionFactoryBuilder setReadLockMode(LockMode lockMode);

    @Override
    BetaTransactionFactoryBuilder setWriteLockMode(LockMode lockMode);

    @Override
    BetaTransactionFactoryBuilder setLockLevel(LockLevel lockLevel);

    @Override
    BetaTransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled);

    @Override
    BetaTransactionFactoryBuilder setSpinCount(int spinCount);

    @Override
    BetaTransactionFactoryBuilder setSpeculativeConfigurationEnabled(boolean enabled);

    @Override
    BetaTransactionFactoryBuilder setReadonly(boolean readonly);

    @Override
    BetaTransactionFactoryBuilder setReadTrackingEnabled(boolean enabled);

    @Override
    BetaTransactionFactoryBuilder setMaxRetries(int maxRetries);

    @Override
    BetaTransactionFactory build();
}
