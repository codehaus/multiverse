package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.DefaultRetryLatch;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.Function;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaObject;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

import static java.lang.String.format;
import static org.multiverse.stms.gamma.GammaStmUtils.toDebugString;

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
    public final RetryLatch listener = new DefaultRetryLatch();


    public GammaTransaction(GammaTransactionConfiguration config, int transactionType) {
        config.init();
        init(config);
        this.transactionType = transactionType;
    }

    public final ReadWriteConflict abortOnReadWriteConflict() {
        abort();
        return ReadWriteConflict.INSTANCE;
    }

    public final ReadonlyException abortOpenForWriteOnReadonly(GammaObject object) {
        abort();
        return new ReadonlyException(
                format("[%s] Failed to Transaction.openForWrite '%s', reason: the transaction is readonly",
                        config.familyName, toDebugString(object)));
    }

    public final IllegalTransactionStateException abortRetryOnNoRetryPossible() {
        abort();
        throw new RetryNotPossibleException(
                format("[%s] Failed to execute Transaction.retry, reason: there are no tracked reads",
                        config.familyName));
    }

    public final RetryNotAllowedException abortRetryOnNoBlockingAllowed() {
        abort();
        return new RetryNotAllowedException(
                format("[%s] Failed to execute Transaction.retry, reason: the transaction doesn't allow blocking",
                        config.familyName));

    }

    public final IllegalTransactionStateException abortRetryOnBadStatus() {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.retry, reason: the transaction is prepared",
                                config.familyName));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.retry, reason: the transaction is aborted",
                                config.familyName));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.retry, reason: the transaction is committed",
                                config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    public final IllegalTransactionStateException abortLocateOnBadStatus(GammaObject object) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.locate '%s' , reason: the transaction is prepared",
                                toDebugString(object), config.familyName));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.locate, '%s' reason: the transaction is aborted",
                                toDebugString(object), config.familyName));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.locate, '%s' reason: the transaction is committed",
                                toDebugString(object), config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    public final NullPointerException abortLocateOnNullArgument() {
        abort();
        return new NullPointerException(
                format("[%s] Failed to execute Transaction.locate, reason: the reference is null",
                        config.familyName));

    }

    public final IllegalTransactionStateException abortOpenForWriteOnBadStatus(GammaObject o) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.openForWrite '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(o)));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.openForWrite '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(o)));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.openForWrite '%s', reason: the transaction is committed",
                                config.familyName, toDebugString(o)));
            default:
                throw new IllegalStateException();
        }
    }

    public final StmMismatchException abortOpenForWriteOnBadStm(GammaObject gammaLongRef) {
        abort();
        //todo: message
        return new StmMismatchException("");
    }


    public final IllegalTransactionStateException abortOpenForReadOnBadStatus(GammaObject object) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.openForRead '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(object)));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.openForRead '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(object)));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.openForRead '%s', reason: the transaction is committed",
                                config.familyName, toDebugString(object)));
            default:
                throw new IllegalStateException();
        }
    }

    public final StmMismatchException abortOpenForReadOnBadStm(GammaObject o) {
        abort();
        //todo: message
        return new StmMismatchException("");
    }


    public final IllegalTransactionStateException abortOpenForConstructionOnBadStatus(GammaObject o) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.openForConstruction '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(o)));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.openForConstruction '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(o)));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.openForConstruction '%s', reason: the transaction is committed",
                                config.familyName, toDebugString(o)));
            default:
                throw new IllegalStateException();
        }
    }

    public final StmMismatchException abortOpenForConstructionOnBadStm(GammaObject o) {
        abort();
        //todo: message
        return new StmMismatchException("");
    }

    public ReadonlyException abortOpenForConstructionOnReadonly(GammaObject o) {
        abort();
        return new ReadonlyException(
                format("[%s] Failed to execute Transaction.openForConstruction '%s', reason: the transaction is readonly",
                        config.familyName, toDebugString(o)));

    }

    public IllegalTransactionStateException abortCommuteOnBadStatus(final GammaObject object, final Function function) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.commute '%s' with reference '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(object), function));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.commute '%s' with reference '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(object), function));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.commute '%s' with reference '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(object), function));
            default:
                throw new IllegalStateException();
        }
    }

    public StmMismatchException abortCommuteOnBadStm(GammaObject gammaLongRef) {
        abort();
        //todo: message
        return new StmMismatchException("");
    }

    public ReadonlyException abortCommuteOnReadonly(final GammaObject object) {
        abort();
        return new ReadonlyException(
                format("[%s] Failed to execute Transaction.commute '%s', reason: the transaction is readonly",
                        config.familyName, toDebugString(object)));
    }

    public NullPointerException abortCommuteOnNullFunction(final GammaObject object) {
        abort();
        return new NullPointerException(
                format("[%s] Failed to execute Transaction.commute '%s', reason: the function is null",
                        config.familyName, toDebugString(object)));
    }


    public final IllegalTransactionStateException abortPrepareOnBadStatus() {
        switch (status) {
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.prepare, reason: the transaction already is aborted",
                                config.familyName));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.prepare, reason: the transaction already is committed",
                                config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    public final IllegalTransactionStateException abortCommitOnBadStatus() {
        return new DeadTransactionException(
                format("[%s] Failed to execute Transaction.commit, reason: the transaction already is aborted",
                        config.familyName));
    }

    public IllegalTransactionStateException abortTryAcquireOnBadStatus(GammaObject object) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.commute '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(object)));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.commute '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(object)));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.commute '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(object)));
            default:
                throw new IllegalStateException();
        }
    }

    public NullPointerException abortTryAcquireOnNullLockMode(GammaObject object) {
        abort();
        return new NullPointerException(
                format("[%s] Failed to execute Lock.tryAcquire '%s', reason: the lockMode is null",
                        config.familyName, toDebugString(object)));
    }


    public IllegalTransactionStateException abortEnsureOnBadStatus(AbstractGammaRef abstractGammaRef) {
        switch (status) {
            case TX_ABORTED:
                return new DeadTransactionException();
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException();
            case TX_COMMITTED:
                return new DeadTransactionException();
            default:
                throw new IllegalStateException();

        }
    }

    public final SpeculativeConfigurationError abortOnTooSmallSize(int minimalSize) {
        config.needsMinimalTransactionLength(minimalSize);
        abort();
        return SpeculativeConfigurationError.INSTANCE;
    }

    public final boolean hasWrites() {
        return hasWrites;
    }

    public abstract void commit();

    public abstract void abort();

    public abstract GammaRefTranlocal locate(AbstractGammaRef o);

    @Override
    public final GammaTransactionConfiguration getConfiguration() {
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
                        format("[%s] Failed to execute Transaction.isAbortOnly, reason: the transaction is committed",
                                config.familyName));
            case TX_ABORTED:
                throw new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.isAbortOnly, reason: the transaction is aborted",
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
                abort();
                throw new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.setAbortOnly, reason: the transaction is prepared",
                                config.familyName));
            case TX_COMMITTED:
                throw new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.setAbortOnly, reason: the transaction is committed",
                                config.familyName));
            case TX_ABORTED:
                throw new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.setAbortOnly, reason: the transaction is aborted",
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

    public abstract GammaRefTranlocal getRefTranlocal(AbstractGammaRef ref);

    public final NullPointerException abortAcquireOnNullLockMode(GammaObject o) {
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
        final long lockEra = listener.getEra();

        if (config.timeoutNs == Long.MAX_VALUE) {
            if (config.isInterruptible()) {
                listener.await(lockEra, config.familyName);
            } else {
                listener.awaitUninterruptible(lockEra);
            }
        } else {
            if (config.isInterruptible()) {
                remainingTimeoutNs = listener.awaitNanos(lockEra, remainingTimeoutNs, config.familyName);
            } else {
                remainingTimeoutNs = listener.awaitNanosUninterruptible(lockEra, remainingTimeoutNs);
            }

            if (remainingTimeoutNs < 0) {
                throw new RetryTimeoutException(
                        format("[%s] Transaction has timed out with a total timeout of %s ns",
                                config.getFamilyName(), config.getTimeoutNs()));
            }
        }
    }

    public final void copyForSpeculativeFailure(GammaTransaction failingTx){
        remainingTimeoutNs = failingTx.remainingTimeoutNs;
        attempt = failingTx.attempt;
    }

    public final void init(GammaTransactionConfiguration config) {
        if (config == null) {
            throw new NullPointerException();
        }
        this.config = config;
        hardReset();
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    public abstract boolean isReadConsistent(GammaRefTranlocal justAdded);

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
}
