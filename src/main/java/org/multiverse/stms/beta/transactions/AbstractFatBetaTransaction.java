package org.multiverse.stms.beta.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;

import java.util.ArrayList;
import java.util.List;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * @author Peter Veentjer
 */
public abstract class AbstractFatBetaTransaction extends BetaTransaction {

    protected ArrayList<TransactionLifecycleListener> permanentListeners;
    protected ArrayList<TransactionLifecycleListener> normalListeners;

    public AbstractFatBetaTransaction(int poolTransactionType, BetaTransactionConfiguration config) {
        super(poolTransactionType, config);
    }

    protected final void notifyListeners(List<TransactionLifecycleListener> listeners, TransactionLifecycleEvent event) {
        if (listeners == null) {
            return;
        }

        int size = listeners.size();
        for (int k = 0; k < size; k++) {
            listeners.get(k).notify(this, event);
        }
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getPermanentListeners() {
        return permanentListeners;
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getNormalListeners() {
        return normalListeners;
    }

    @Override
    public final void copyForSpeculativeFailure(BetaTransaction tx) {
        List<TransactionLifecycleListener> listeners = tx.getPermanentListeners();

        if (listeners != null) {
            if (permanentListeners == null) {
                permanentListeners = new ArrayList<TransactionLifecycleListener>(listeners.size());
            }

            permanentListeners.addAll(listeners);
        }

        remainingTimeoutNs = tx.getRemainingTimeoutNs();
        attempt = tx.getAttempt();
    }


    public final void registerPermanent(final BetaObjectPool pool, final TransactionLifecycleListener listener) {
        if (listener == null) {
            abort();
            throw new NullPointerException();
        }

        switch (status) {
            case NEW:
            case ACTIVE:
            case PREPARED:
                if (permanentListeners == null) {
                    permanentListeners = pool.takeArrayList();
                    if (permanentListeners == null) {
                        permanentListeners = new ArrayList<TransactionLifecycleListener>();
                    }
                }
                permanentListeners.add(listener);
                break;
            case COMMITTED:
                throw new DeadTransactionException();
            case ABORTED:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
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
                if (normalListeners == null) {
                    normalListeners = pool.takeArrayList();
                    if (normalListeners == null) {
                        normalListeners = new ArrayList<TransactionLifecycleListener>();
                    }
                }
                normalListeners.add(listener);
                break;
            case COMMITTED:
                throw new DeadTransactionException();
            case ABORTED:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }  
}
