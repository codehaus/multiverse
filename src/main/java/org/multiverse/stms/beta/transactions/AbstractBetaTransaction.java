package org.multiverse.stms.beta.transactions;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.Listeners;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.ObjectPool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public abstract class AbstractBetaTransaction implements BetaTransaction {
    public final static int NEW = 0;
    public final static int ACTIVE = 1;
    public final static int PREPARED = 2;
    public final static int ABORTED = 3;
    public final static int COMMITTED = 4;

    protected int status = ACTIVE;

    protected Object permanentListeners;
    protected Object normalListeners;
    protected int attempt = 1;

    public void notifyListeners(Object listeners, TransactionLifecycleEvent event) {
        if (listeners == null) {
            return;
        }

        if (listeners instanceof TransactionLifecycleListener) {
            ((TransactionLifecycleListener) listeners).notify(this, event);
            return;
        }

        ArrayList<TransactionLifecycleListener> list = (ArrayList) listeners;
        for (int k = 0; k < list.size(); k++) {
            list.get(k).notify(this, event);
        }
    }

    @Override
    public final void incAttempt() {
        attempt++;
    }

    @Override
    public final int getAttempt() {
        return attempt;
    }

    @Override
    public final void resetAttempt() {
        attempt = 1;
    }

    /**
     * Opens all the listeners. As soon as in the array a null element is found, it signals the end of
     * the list of listeners. This makes is possible to place an array that is larger than the actual
     * number of writes.
     *
     * @param listenersArray
     * @param pool
     */
    public void openListeners(final Listeners[] listenersArray, final ObjectPool pool) {
        if (listenersArray == null) {
            return;
        }

        for (int k = 0; k < listenersArray.length; k++) {
            Listeners listeners = listenersArray[k];

            if (listeners == null) {
                return;
            }

            listenersArray[k] = null;
            do {
                Listeners next = listeners.next;
                listeners.listener.open(listeners.listenerEra);
                pool.putListeners(listeners);
                listeners = next;
            } while (listeners != null);
        }
    }

    public void openListeners(Listeners listeners, final ObjectPool pool) {
        while (listeners != null) {
            Listeners next = listeners.next;
            listeners.listener.open(listeners.listenerEra);
            pool.putListeners(listeners);
            listeners = next;
        }
    }

    @Override
    public void registerPermanent(TransactionLifecycleListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }

        switch (status) {
            case NEW:
            case ACTIVE:
            case PREPARED:
                if (permanentListeners == null) {
                    permanentListeners = listener;
                } else if (permanentListeners instanceof ArrayList) {
                    ((ArrayList) permanentListeners).add(listener);
                } else {
                    List listeners = new ArrayList();
                    listeners.add(permanentListeners);
                    listeners.add(listener);
                    permanentListeners = listeners;
                }
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
        if (listener == null) {
            throw new NullPointerException();
        }

        switch (status) {
            case NEW:
            case ACTIVE:
            case PREPARED:
                if (normalListeners == null) {
                    normalListeners = listener;
                } else if (normalListeners instanceof ArrayList) {
                    ((ArrayList) normalListeners).add(listener);
                } else {
                    List listeners = new ArrayList();
                    listeners.add(normalListeners);
                    listeners.add(listener);
                    normalListeners = listeners;
                }
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
    public final TransactionStatus getStatus() {
        switch (status) {
            case NEW:
                return TransactionStatus.New;
            case ACTIVE:
                return TransactionStatus.Active;
            case ABORTED:
                return TransactionStatus.Aborted;
            case COMMITTED:
                return TransactionStatus.Committed;
            case PREPARED:
                return TransactionStatus.Prepared;
            default:
                throw new IllegalStateException();
        }
    }

    protected final ReadConflict abortOnReadConflict(ObjectPool pool) {
        abort(pool);
        return ReadConflict.INSTANCE;
    }

    protected final SpeculativeConfigurationError abortOnTooSmallSize(ObjectPool pool) {
        abort(pool);
        return SpeculativeConfigurationError.INSTANCE;
    }

    protected final WriteConflict abortOnWriteConflict(ObjectPool pool) {
        abort(pool);
        return WriteConflict.INSTANCE;
    }
}
