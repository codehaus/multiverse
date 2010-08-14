package org.multiverse.stms.beta.transactions;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;

import java.util.ArrayList;
import java.util.List;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public abstract class AbstractLeanBetaTransaction implements BetaTransaction {
    public final static int NEW = 0;
    public final static int ACTIVE = 1;
    public final static int PREPARED = 2;
    public final static int ABORTED = 3;
    public final static int COMMITTED = 4;

    private final int poolTransactionType;        
    protected int status = ACTIVE;
    protected int attempt = 1;
    protected long remainingTimeoutNs;
    protected BetaTransactionConfig config;
    protected boolean abortOnly;

    public AbstractLeanBetaTransaction(int poolTransactionType, BetaTransactionConfig config) {
        this.poolTransactionType = poolTransactionType;
        this.config = config;
    }

    @Override
    public int getPoolTransactionType() {
        return poolTransactionType;
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
    public final BetaTransactionConfig getConfiguration() {
        return config;
    }

    @Override
    public final int getAttempt() {
        return attempt;
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getPermanentListeners() {
        return null;
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getNormalListeners() {
        return null;
    }

    public final void copyForSpeculativeFailure(BetaTransaction tx) {
        List<TransactionLifecycleListener> listeners = tx.getPermanentListeners();

        if (listeners != null) {
            throw new TodoException();
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
        //we can forward to the normal register call since a speculative failure is going to be thrown.
        register(pool, listener);
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
