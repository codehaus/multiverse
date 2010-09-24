package org.multiverse.stms.beta.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public abstract class AbstractFatBetaTransaction extends BetaTransaction {

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
    public final ArrayList<TransactionLifecycleListener> getNormalListeners() {
        return normalListeners;
    }

    @Override
    public final void copyForSpeculativeFailure(BetaTransaction tx) {
        remainingTimeoutNs = tx.getRemainingTimeoutNs();
        attempt = tx.getAttempt();
    }

    @Override
    public final void register(final TransactionLifecycleListener listener) {
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
