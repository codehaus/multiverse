package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.DefaultRetryLatch;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.Function;
import org.multiverse.api.lifecycle.TransactionListener;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaObject;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

import java.util.List;

import static java.lang.String.format;
import static org.multiverse.stms.gamma.GammaStmUtils.toDebugString;

public abstract class GammaTransaction implements GammaConstants, Transaction {

    public final GammaObjectPool pool = new GammaObjectPool();
    public int status = TX_ACTIVE;
    public GammaTransactionConfiguration config;
    public int attempt;
    public long remainingTimeoutNs;
    public boolean hasWrites;
    public final int transactionType;
    public boolean poorMansConflictScan;

    public boolean abortOnly = false;
    public final RetryLatch retryListener = new DefaultRetryLatch();
    public List<TransactionListener> listeners;

    public GammaTransaction(GammaTransactionConfiguration config, int transactionType) {
        config.init();
        init(config);
        this.transactionType = transactionType;
    }

    public final void abortIfAlive() {
        if (isAlive()) {
            abort();
        }
    }

    public final ReadWriteConflict abortOnReadWriteConflict() {
        abortIfAlive();
        if (config.controlFlowErrorsReused) {
            return ReadWriteConflict.INSTANCE;
        } else {
            return new ReadWriteConflict();
        }
    }

    // ================= open for read =============================

    public SpeculativeConfigurationError abortOpenForReadOrWriteOnExplicitLocking(AbstractGammaRef ref) {
        config.updateSpeculativeConfigurationToUseExplicitLocking();
        abortIfAlive();
        if (config.controlFlowErrorsReused) {
            return SpeculativeConfigurationError.INSTANCE;
        }
        return new SpeculativeConfigurationError(
                format("[%s] Failed to execute Ref.openForRead/openForWrite '%s', reason: the transaction is lean, " +
                        "but explicit locking is required",
                        config.familyName, toDebugString(ref)));
    }

    public SpeculativeConfigurationError abortOpenForReadOrWriteOnNonRefType(AbstractGammaRef ref) {
        config.updateSpeculativeConfigurationToUseNonRefType();
        abortIfAlive();
        if (config.controlFlowErrorsReused) {
            return SpeculativeConfigurationError.INSTANCE;
        }
        return new SpeculativeConfigurationError(
                format("[%s] Failed to execute Ref.openForRead/openForWrite '%s', reason: the transaction is lean," +
                        " but explicit locking is required",
                        config.familyName, toDebugString(ref)));

    }

    public final StmMismatchException abortOpenForReadOnBadStm(GammaObject o) {
        abortIfAlive();
        return new StmMismatchException(
                format("[%s] Failed to execute Ref.openForRead '%s', reason: the stm the ref was created " +
                        "with is a different stm than the stm of the transaction",
                        config.familyName, toDebugString(o)));
    }

    public final IllegalTransactionStateException abortOpenForReadOnBadStatus(GammaObject object) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Ref.openForRead '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(object)));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Ref.openForRead '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(object)));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Ref.openForRead '%s', reason: the transaction is committed",
                                config.familyName, toDebugString(object)));
            default:
                throw new IllegalStateException();
        }
    }

    // ============== open for write ============================

    public final ReadonlyException abortOpenForWriteOnReadonly(GammaObject object) {
        abortIfAlive();
        return new ReadonlyException(
                format("[%s] Failed to Ref.openForWrite '%s', reason: the transaction is readonly",
                        config.familyName, toDebugString(object)));
    }

    public final IllegalTransactionStateException abortOpenForWriteOnBadStatus(GammaObject o) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Ref.openForWrite '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(o)));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Ref.openForWrite '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(o)));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Ref.openForWrite '%s', reason: the transaction is committed",
                                config.familyName, toDebugString(o)));
            default:
                throw new IllegalStateException();
        }
    }

    public final StmMismatchException abortOpenForWriteOnBadStm(GammaObject o) {
        abortIfAlive();
        return new StmMismatchException(
                format("[%s] Failed to execute Ref.openForWrite '%s', reason: the stm the ref was created with " +
                        "is a different stm than the stm of the transaction",
                        config.familyName, toDebugString(o)));
    }

    // ============================= retry ==============================

    public final IllegalTransactionStateException abortRetryOnNoRetryPossible() {
        abortIfAlive();
        throw new RetryNotPossibleException(
                format("[%s] Failed to execute Ref.retry, reason: there are no tracked reads",
                        config.familyName));
    }

    public final RetryNotAllowedException abortRetryOnNoBlockingAllowed() {
        abortIfAlive();
        return new RetryNotAllowedException(
                format("[%s] Failed to execute Ref.retry, reason: the transaction doesn't allow blocking",
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

    // ==========================  open for construction ===========================


    public final IllegalArgumentException abortOpenForConstructionOnBadReference(
            final GammaObject ref) {

        abortIfAlive();
        return new IllegalArgumentException(
                format("[%s] Failed to execute Ref.openForConstruction '%s', reason: the object is not new " +
                        "and has previous commits",
                        config.familyName, toDebugString(ref)));
    }


    public final IllegalTransactionStateException abortOpenForConstructionOnBadStatus(GammaObject o) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Ref.openForConstruction '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(o)));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Ref.openForConstruction '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(o)));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Ref.openForConstruction '%s', reason: the transaction is committed",
                                config.familyName, toDebugString(o)));
            default:
                throw new IllegalStateException();
        }
    }

    public final StmMismatchException abortOpenForConstructionOnBadStm(GammaObject o) {
        abortIfAlive();
        return new StmMismatchException(
                format("[%s] Failed to execute Ref.openForConstruction '%s', reason: the stm the ref was " +
                        "created with is a different stm than the stm of the transaction",
                        config.familyName, toDebugString(o)));

    }

    public ReadonlyException abortOpenForConstructionOnReadonly(GammaObject o) {
        abortIfAlive();
        return new ReadonlyException(
                format("[%s] Failed to execute Ref.openForConstruction '%s', reason: the transaction is readonly",
                        config.familyName, toDebugString(o)));

    }

    public SpeculativeConfigurationError abortOpenForConstructionRequired(AbstractGammaRef ref) {
        config.updateSpeculativeConfigurationToUseConstructedObjects();
        abortIfAlive();
        if (config.controlFlowErrorsReused) {
            return SpeculativeConfigurationError.INSTANCE;
        }
        return new SpeculativeConfigurationError(
                format("[%s] Failed to execute Ref.openForConstruction '%s', reason: the transaction is lean, " +
                        "but explicit attachments of constructed objects is required",
                        config.familyName, toDebugString(ref)));
    }

    // ============================== open for commute ======================

    public SpeculativeConfigurationError abortCommuteOnCommuteRequired(AbstractGammaRef ref) {
        config.updateSpeculativeConfigurationToUseCommute();
        abortIfAlive();
        if (config.controlFlowErrorsReused) {
            return SpeculativeConfigurationError.INSTANCE;
        }
        return new SpeculativeConfigurationError(
                format("[%s] Failed to execute Ref.commute '%s', reason: the transaction is lean, but commute is required",
                        config.familyName, toDebugString(ref)));
    }

    public IllegalTransactionStateException abortCommuteOnBadStatus(final GammaObject object, final Function function) {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Ref.commute '%s' with reference '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(object), function));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Ref.commute '%s' with reference '%s', reason: the transaction is aborted",
                                config.familyName, toDebugString(object), function));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Ref.commute '%s' with reference '%s', reason: the transaction is prepared",
                                config.familyName, toDebugString(object), function));
            default:
                throw new IllegalStateException();
        }
    }

    public StmMismatchException abortCommuteOnBadStm(GammaObject object) {
        abortIfAlive();
        return new StmMismatchException(
                format("[%s] Failed to execute Ref.commute '%s', reason: the stm the ref was created with is a different" +
                        " stm than the stm of the transaction",
                        config.familyName, toDebugString(object)));
    }

    public ReadonlyException abortCommuteOnReadonly(final GammaObject object) {
        abortIfAlive();
        return new ReadonlyException(
                format("[%s] Failed to execute Ref.commute '%s', reason: the transaction is readonly",
                        config.familyName, toDebugString(object)));
    }

    public NullPointerException abortCommuteOnNullFunction(final GammaObject object) {
        abortIfAlive();
        return new NullPointerException(
                format("[%s] Failed to execute Ref.commute '%s', reason: the function is null",
                        config.familyName, toDebugString(object)));
    }

    // ==========================================

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
        abortIfAlive();
        return new NullPointerException(
                format("[%s] Failed to execute Transaction.locate, reason: the reference is null",
                        config.familyName));

    }

    // ====================== register ==========================================

    private NullPointerException abortRegisterOnNullListener() {
        abortIfAlive();
        return new NullPointerException(
                format("[%s] Failed to execute Transaction.register , reason: the listener is null",
                        config.familyName));
    }

    private IllegalTransactionStateException abortRegisterOnBadStatus() {
        switch (status) {
            case TX_PREPARED:
                abort();
                return new PreparedTransactionException(
                        format("[%s] Failed to execute Transaction.register, reason: the transaction is prepared",
                                config.familyName));
            case TX_ABORTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.register, reason: the transaction is aborted",
                                config.familyName));
            case TX_COMMITTED:
                return new DeadTransactionException(
                        format("[%s] Failed to execute Transaction.register, reason: the transaction is prepared",
                                config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    public SpeculativeConfigurationError abortRegisterOnListenerRequired() {
        config.updateSpeculativeConfigurationToUseListeners();
        abortIfAlive();
        if (config.controlFlowErrorsReused) {
            return SpeculativeConfigurationError.INSTANCE;
        }
        return new SpeculativeConfigurationError(
                format("[%s] Failed to execute Transaction.register, reason: the transaction is lean, but listeners are required",
                        config.familyName));
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
        abortIfAlive();
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
        abortIfAlive();
        return new NullPointerException(
                format("[%s] Failed to execute Lock.tryAcquire '%s', reason: the lockMode is null",
                        config.familyName, toDebugString(object)));
    }


    public IllegalTransactionStateException abortEnsureOnBadStatus(AbstractGammaRef o) {
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

    public final SpeculativeConfigurationError abortOnTransactionTooSmall(int minimalSize) {
        //todo:
        config.updateSpeculativeConfigurationToUseMinimalTransactionLength(minimalSize);
        abortIfAlive();
        if (config.controlFlowErrorsReused) {
            return SpeculativeConfigurationError.INSTANCE;
        }

        return new SpeculativeConfigurationError();
    }

    public final SpeculativeConfigurationError abortOnTransactionTooLargeForPoorMansConflictScan() {
        config.updateSpeculativeConfigurationToUseRichMansConflictScan();
        abortIfAlive();
        if (config.controlFlowErrorsReused) {
            return SpeculativeConfigurationError.INSTANCE;
        }
        //todo: correct message
        return new SpeculativeConfigurationError();
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
    public void register(TransactionListener listener) {
        if (listener == null) {
            throw abortRegisterOnNullListener();
        }

        if (status != TX_ACTIVE) {
            throw abortRegisterOnBadStatus();
        }

        if (transactionType == TRANSACTIONTYPE_LEAN_MONO || transactionType == TRANSACTIONTYPE_LEAN_FIXED_LENGTH) {
            throw abortRegisterOnListenerRequired();
        }

        if (listeners == null) {
            listeners = pool.takeArrayList();
        }

        listeners.add(listener);
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
        final long lockEra = retryListener.getEra();

        if (config.timeoutNs == Long.MAX_VALUE) {
            if (config.isInterruptible()) {
                retryListener.await(lockEra, config.familyName);
            } else {
                retryListener.awaitUninterruptible(lockEra);
            }
        } else {
            if (config.isInterruptible()) {
                remainingTimeoutNs = retryListener.awaitNanos(lockEra, remainingTimeoutNs, config.familyName);
            } else {
                remainingTimeoutNs = retryListener.awaitNanosUninterruptible(lockEra, remainingTimeoutNs);
            }

            if (remainingTimeoutNs < 0) {
                throw new RetryTimeoutException(
                        format("[%s] Transaction has timed out with a total timeout of %s ns",
                                config.getFamilyName(), config.getTimeoutNs()));
            }
        }
    }

    public final void copyForSpeculativeFailure(GammaTransaction failingTx) {
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
