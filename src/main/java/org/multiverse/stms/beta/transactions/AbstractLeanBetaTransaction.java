package org.multiverse.stms.beta.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;

import java.util.ArrayList;
import java.util.List;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public abstract class AbstractLeanBetaTransaction extends BetaTransaction {

    public AbstractLeanBetaTransaction(int poolTransactionType, BetaTransactionConfiguration config) {
        super(poolTransactionType, config);
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getPermanentListeners() {
        return null;
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getNormalListeners() {
        return null;
    }

    @Override
    public final void copyForSpeculativeFailure(BetaTransaction tx) {
        List<TransactionLifecycleListener> listeners = tx.getPermanentListeners();

        if (listeners != null) {
            throw new TodoException();
        }

        remainingTimeoutNs = tx.getRemainingTimeoutNs();
        attempt = tx.getAttempt();
    }


    public final void registerPermanent(final BetaObjectPool pool, final TransactionLifecycleListener listener) {
        //we can forward to the normal register call since a speculative failure is going to be thrown.
        register(pool, listener);
    }

    @Override
    public final void register(TransactionLifecycleListener listener) {
        register(getThreadLocalBetaObjectPool(), listener);
    }

    @Override
    public final void register(final BetaObjectPool pool, final TransactionLifecycleListener listener) {
        if (listener == null) {
            abort();
            throw new NullPointerException();
        }

        switch (status) {
            case NEW:
            case ACTIVE:
            case PREPARED:
                config.needsListeners();
                abort();
                throw SpeculativeConfigurationError.INSTANCE;
            case COMMITTED:
                throw new DeadTransactionException();
            case ABORTED:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }
}
