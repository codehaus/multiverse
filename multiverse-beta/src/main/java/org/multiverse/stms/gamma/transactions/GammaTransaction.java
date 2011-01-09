package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionConfiguration;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaObject;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

public abstract class GammaTransaction implements GammaConstants, Transaction {

    public int status = TX_ACTIVE;
    public GammaTransactionConfiguration config;
    public int attempt;
    public long remainingTimeoutNs;
    public boolean hasWrites;
    public final GammaObjectPool pool = new GammaObjectPool();

    public GammaTransaction(GammaTransactionConfiguration config) {
        this.config = config;
    }

    public final ReadWriteConflict abortOnReadWriteConflict() {
        abort();
        return ReadWriteConflict.INSTANCE;
    }

    public final ReadonlyException abortOpenForWriteOnReadonly() {
        abort();
        return new ReadonlyException();
    }

    public final NullPointerException abortLocateOnNullArgument() {
        abort();
        throw new NullPointerException();
    }

    public RetryNotAllowedException abortRetryOnNoBlockingAllowed() {
        abort();
        return new RetryNotAllowedException("");
    }

    public IllegalTransactionStateException abortRetryOnBadStatus() {
        switch (status) {
            case TX_ABORTED:
                return new DeadTransactionException();
            case TX_COMMITTED:
                return new DeadTransactionException();
            default:
                throw new IllegalStateException();

        }
    }

    public final IllegalTransactionStateException abortLocateOnBadStatus() {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException();
            case TX_ABORTED:
                return new DeadTransactionException();
            case TX_COMMITTED:
                return new DeadTransactionException();
            default:
                throw new IllegalStateException();

        }
    }

    public final TransactionStatus getStatus() {
        switch (status) {
            case TX_ACTIVE:
                return TransactionStatus.Active;
            case TX_PREPARED:
                return TransactionStatus.Prepared;
            case TX_COMMITTED:
                return TransactionStatus.Committed;
            case TX_ABORTED:
                return TransactionStatus.Aborted;
            default:
                throw new IllegalStateException();
        }
    }

    public final SpeculativeConfigurationError abortOnTransactionTooSmall() {
        abort();
        return SpeculativeConfigurationError.INSTANCE;
    }

    public final IllegalTransactionStateException abortOpenForWriteOnBadStatus() {
        switch (status) {
            case TX_ABORTED:
                return new DeadTransactionException();
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException();
            case TX_COMMITTED:
                abort();
                return new DeadTransactionException();
            default:
                throw new IllegalStateException();

        }
    }

    public final IllegalTransactionStateException abortPrepareOnBadStatus() {
        switch (status) {
            case TX_ABORTED:
                return new DeadTransactionException();
            case TX_COMMITTED:
                abort();
                return new DeadTransactionException();
            default:
                throw new IllegalStateException();

        }
    }

    public final IllegalTransactionStateException abortOpenForReadOnBadStatus() {
        switch (status) {
            case TX_ABORTED:
                return new DeadTransactionException();
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException();
            case TX_COMMITTED:
                abort();
                return new DeadTransactionException();
            default:
                throw new IllegalStateException();

        }
    }

    public final IllegalTransactionStateException abortCommitOnBadStatus() {
        switch (status) {
            case TX_ABORTED:
                return new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }

    public final boolean hasWrites() {
        return hasWrites;
    }

    public abstract void commit();

    public abstract void abort();

    public abstract GammaTranlocal locate(GammaObject o);

    public abstract GammaTranlocal openForRead(GammaLongRef o, int lockMode);

    public abstract GammaTranlocal openForWrite(GammaLongRef o, int lockMode);

    @Override
    public final TransactionConfiguration getConfiguration() {
        return config;
    }

    @Override
    public final int getAttempt() {
        return attempt;
    }

    @Override
    public final long getRemainingTimeoutNs() {
        return remainingTimeoutNs;
    }

    @Override
    public final void setAbortOnly() {
        throw new TodoException();
    }

    @Override
    public void ensureWrites() {
        throw new TodoException();
    }

    @Override
    public void register(TransactionLifecycleListener listener) {
        throw new TodoException();
    }

    public abstract void reset();

    public abstract GammaTranlocal get(GammaObject ref);

    public final NullPointerException abortOnNullLockMode(){
        switch (status){
            case TX_ACTIVE:
                abort();
                return new NullPointerException();
            case TX_PREPARED:
                abort();
                return new NullPointerException();
            case TX_ABORTED:
                return new NullPointerException();
            case TX_COMMITTED:
                return new NullPointerException();
            default:
                throw new IllegalStateException();
        }
    }
}
