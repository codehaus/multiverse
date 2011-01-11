package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionConfiguration;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaObject;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

import static java.lang.String.format;

public abstract class GammaTransaction implements GammaConstants, Transaction {

    public final static int POOL_TRANSACTIONTYPE_MONO = 1;
    public final static int POOL_TRANSACTIONTYPE_ARRAY = 2;
    public final static int POOL_TRANSACTIONTYPE_MAP = 3;

    public final GammaObjectPool pool = new GammaObjectPool();
    public int status = TX_ACTIVE;
    public GammaTransactionConfiguration config;
    public int attempt;
    public long remainingTimeoutNs;
    public boolean hasWrites;
    public boolean arriveEnabled;
    public final int transactionType;
    public boolean abortOnly = false;

    public GammaTransaction(GammaTransactionConfiguration config, int transactionType) {
        this.config = config;
        this.transactionType = transactionType;
    }

    public abstract void commute(GammaLongRef ref, LongFunction function);

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
    public boolean isAbortOnly() {
        switch (status) {
            case TX_ACTIVE:
                return abortOnly;
            case TX_PREPARED:
                return abortOnly;
            case TX_COMMITTED:
                throw new DeadTransactionException(
                        format("[%s] Failed to execute BetaTransaction.setAbortOnly, reason: the transaction is committed",
                                config.familyName));
            case TX_ABORTED:
                throw new DeadTransactionException(
                        format("[%s] Failed to execute BetaTransaction.setAbortOnly, reason: the transaction is aborted",
                                config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public final void setAbortOnly() {
        switch (status) {
            case TX_ACTIVE:
                abortOnly = true;
                break;
            case TX_PREPARED:
                throw new PreparedTransactionException(
                        format("[%s] Failed to execute BetaTransaction.setAbortOnly, reason: the transaction is prepared",
                                config.familyName));
            case TX_COMMITTED:
                throw new DeadTransactionException(
                        format("[%s] Failed to execute BetaTransaction.setAbortOnly, reason: the transaction is committed",
                                config.familyName));
            case TX_ABORTED:
                throw new DeadTransactionException(
                        format("[%s] Failed to execute BetaTransaction.setAbortOnly, reason: the transaction is aborted",
                                config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void ensureWrites() {
        throw new TodoException();
    }

    @Override
    public void register(TransactionLifecycleListener listener) {
        throw new TodoException();
    }

    public abstract void hardReset();

    public abstract boolean softReset();

    public abstract GammaTranlocal get(GammaObject ref);

    public final NullPointerException abortOnNullLockMode() {
        switch (status) {
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

    public final boolean isAlive() {
        return status == TX_ACTIVE || status == TX_PREPARED;
    }

    public final void awaitUpdate() {
        throw new TodoException();
    }

    public abstract void copyForSpeculativeFailure(GammaTransaction failingTx);

    public void openForConstruction() {
        throw new TodoException();
    }
}
