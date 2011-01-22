package org.multiverse.stms.beta.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public abstract class AbstractFatBetaTransaction extends BetaTransaction {

    protected ArrayList<TransactionListener> normalListeners;

    public AbstractFatBetaTransaction(int poolTransactionType, BetaTransactionConfiguration config) {
        super(poolTransactionType, config);
    }

    protected final void notifyListeners(List<TransactionListener> listeners, TransactionLifecycleEvent event) {
        if (listeners == null) {
            return;
        }

        int size = listeners.size();
        for (int k = 0; k < size; k++) {
            listeners.get(k).notify(this, event);
        }
    }

    @Override
    public final ArrayList<TransactionListener> getNormalListeners() {
        return normalListeners;
    }

    @Override
    public final void copyForSpeculativeFailure(BetaTransaction tx) {
        remainingTimeoutNs = tx.getRemainingTimeoutNs();
        attempt = tx.getAttempt();
    }

    @Override
    public final void register(final TransactionListener listener) {
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
