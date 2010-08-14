package org.multiverse.stms.beta.transactions;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;

import java.util.ArrayList;
import java.util.List;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * @author Peter Veentjer
 */
public abstract class AbstractFatBetaTransaction implements BetaTransaction {
    public final static int NEW = 0;
    public final static int ACTIVE = 1;
    public final static int PREPARED = 2;
    public final static int ABORTED = 3;
    public final static int COMMITTED = 4;

    protected BetaTransactionConfig config;
    protected int status = ACTIVE;

    protected ArrayList<TransactionLifecycleListener> permanentListeners;
    protected ArrayList<TransactionLifecycleListener> normalListeners;
    protected int attempt = 1;
    protected long remainingTimeoutNs;
    protected boolean abortOnly;
    private final int poolTransactionType;

    public AbstractFatBetaTransaction(int poolTransactionType, BetaTransactionConfig config) {
        this.poolTransactionType = poolTransactionType;
        this.config = config;
    }

    @Override
    public int getPoolTransactionType() {
        return poolTransactionType;
    }

    @Override
    public final BetaTransactionConfig getConfiguration() {
        return config;
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

    public final void setAbortOnly() {
        switch (status) {
            case NEW:
                throw new TodoException();
            case ACTIVE:
                abortOnly = true;
                break;
            case PREPARED:
                throw new PreparedTransactionException();
            case COMMITTED:
                throw new DeadTransactionException();
            case ABORTED:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public final int getAttempt() {
        return attempt;
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getPermanentListeners() {
        return permanentListeners;
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getNormalListeners() {
        return normalListeners;
    }

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


    @Override
    public void registerPermanent(TransactionLifecycleListener listener) {
        registerPermanent(getThreadLocalBetaObjectPool(), listener);
    }

    @Override
    public void registerPermanent(final BetaObjectPool pool, final TransactionLifecycleListener listener) {
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
    public void register(final BetaObjectPool pool, final TransactionLifecycleListener listener) {
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

    @Override
    public long getRemainingTimeoutNs() {
        return remainingTimeoutNs;
    }

    @Override
    public void setRemainingTimeoutNs(long timeoutNs) {
        if (timeoutNs > remainingTimeoutNs) {
            throw new IllegalArgumentException();
        }
        this.remainingTimeoutNs = timeoutNs;
    }

    protected final ReadConflict abortOnReadConflict(BetaObjectPool pool) {
        abort(pool);
        return ReadConflict.INSTANCE;
    }

    protected final SpeculativeConfigurationError abortOnTooSmallSize(BetaObjectPool pool, int minimalSize) {
        config.needsMinimalTransactionLength(minimalSize);
        abort(pool);
        return SpeculativeConfigurationError.INSTANCE;
    }

    protected final WriteConflict abortOnWriteConflict(BetaObjectPool pool) {
        abort(pool);
        return WriteConflict.INSTANCE;
    }
}
