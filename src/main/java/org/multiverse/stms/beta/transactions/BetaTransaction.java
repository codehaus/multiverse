package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.Watch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.Function;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.transactionalobjects.*;

import java.util.ArrayList;

import static java.lang.String.format;
import static org.multiverse.stms.beta.BetaStmUtils.toDebugString;

/**
 * @author Peter Veentjer
 */
public abstract class BetaTransaction implements Transaction {

    public final static int POOL_TRANSACTIONTYPE_LEAN_MONO = 0;
    public final static int POOL_TRANSACTIONTYPE_FAT_MONO = 1;
    public final static int POOL_TRANSACTIONTYPE_LEAN_ARRAY = 2;
    public final static int POOL_TRANSACTIONTYPE_FAT_ARRAY = 3;
    public final static int POOL_TRANSACTIONTYPE_LEAN_ARRAYTREE = 4;
    public final static int POOL_TRANSACTIONTYPE_FAT_ARRAYTREE = 5;

    public final static int NEW = 0;
    public final static int ACTIVE = 1;
    public final static int PREPARED = 2;
    public final static int ABORTED = 3;
    public final static int COMMITTED = 4;

    private final int poolTransactionType;
    protected int status = ACTIVE;
    protected int attempt = 1;
    protected long remainingTimeoutNs;
    protected BetaTransactionConfiguration config;
    protected boolean abortOnly;

    public BetaTransaction(int poolTransactionType, BetaTransactionConfiguration config) {
        this.poolTransactionType = poolTransactionType;
        this.config = config;
    }

    public final int getPoolTransactionType() {
        return poolTransactionType;
    }

    @Override
    public final void setAbortOnly() {
        switch (status) {
            case NEW:
                throw new TodoException();
            case ACTIVE:
                abortOnly = true;
                break;
            case PREPARED:
                throw new PreparedTransactionException(
                    format("[%s] Can't setAbortOnly on an already prepared transaction",
                        config.familyName));
            case COMMITTED:
                throw new DeadTransactionException(
                    format("[%s] Can't setAbortOnly on an already committed transaction",
                        config.familyName));
            case ABORTED:
                throw new DeadTransactionException(
                    format("[%s] Can't setAbortOnly on an already aborted transaction",
                        config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    public final NoRetryPossibleException abortOnNoRetryPossible(final BetaObjectPool pool){
        abort(pool);
        throw new NoRetryPossibleException(
            format("[%s] Can't block transaction since there are no tracked reads",
                config.familyName));
    }

    public final NoRetryPossibleException abortOnNoBlockingAllowed(final BetaObjectPool pool){
        abort(pool);
        return new NoRetryPossibleException(
            format("[%s] Can't block transaction since it doesn't allow blocking",
                config.familyName));
    }

    public final IllegalTransactionStateException abortOnFaultyStatusOfRegisterChangeListenerAndAbort(final BetaObjectPool pool){
        switch (status) {
            case PREPARED:
                abort(pool);
                return new PreparedTransactionException(
                    format("[%s] Can't block on an already prepared transaction",
                        config.familyName));
            case ABORTED:
                return new DeadTransactionException(
                    format("[%s] Can't block on already aborted transaction",
                        config.familyName));
            case COMMITTED:
                return new DeadTransactionException(
                    format("[%s] Can't block on already committed transaction",
                        config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    public final IllegalArgumentException abortOpenForConstructionWithBadReference(
        final BetaObjectPool pool, final BetaTransactionalObject ref){

        abort(pool);
        return new IllegalArgumentException(
            format("[%s] Can't openForConstruction a previous committed object or an object '%s'",
                config.familyName, toDebugString(ref)));
    }

    public final ReadonlyException abortOpenForWriteWhenReadonly(
        final BetaObjectPool pool, final BetaTransactionalObject object){

        abort(pool);
        return new ReadonlyException(
            format("[%s] Can't openForWrite '%s' on a readonly transaction",
                config.familyName, toDebugString(object)));
    }

    public final NullPointerException abortOpenForWriteWhenNullReference(final BetaObjectPool pool){
        abort(pool);
        return new NullPointerException(
            format("[%s] Can't openForWrite a null reference",
                config.familyName));
    }

    public final NullPointerException abortOpenForConstructionWhenNullReference(final BetaObjectPool pool){
        abort(pool);
        return new NullPointerException(
            format("[%s] Can't openForConstruction a null reference",
                config.familyName));
    }

    public final NullPointerException abortCommuteWhenNullReference(
        final BetaObjectPool pool, Function function){

        abort(pool);
        return new NullPointerException(
            format("[%s] Can't commute with a null reference and function '%s'",
                config.familyName, function));
    }

    public final ReadonlyException abortOpenForConstructionWhenReadonly(
        final BetaObjectPool pool, final BetaTransactionalObject object){

        abort(pool);
        return new ReadonlyException(
            format("[%s] Can't openForConstruction '%s' using a readonly transaction",
                config.familyName, toDebugString(object)));
    }

    public final ReadonlyException abortCommuteWhenReadonly(
            final BetaObjectPool pool, final BetaTransactionalObject object, final Function function){

        abort(pool);
        return new ReadonlyException(
            format("[%s] Can't commute on '%s' with function '%s' and a readonly transaction ''",
                 config.familyName, toDebugString(object), function));
    }

    public final IllegalTransactionStateException abortOpenForRead(final BetaObjectPool pool, final BetaTransactionalObject object) {
        switch (status) {
            case PREPARED:
                abort(pool);
                return new PreparedTransactionException(
                    format("[%s] Can't openForRead '%s' using an already prepared transaction",
                        config.familyName, toDebugString(object)));
            case ABORTED:
                return new DeadTransactionException(
                    format("[%s] Can't openForRead '%s' using an already aborted transaction",
                        config.familyName, toDebugString(object)));
            case COMMITTED:
                return new DeadTransactionException(
                    format("[%s] Can't openForRead '%s' using already committed transaction",
                        config.familyName, toDebugString(object)));
            default:
                throw new IllegalStateException();
        }
    }

    public final IllegalTransactionStateException abortOpenForWrite(
        final BetaObjectPool pool, final BetaTransactionalObject object) {

        switch (status) {
            case PREPARED:
                abort(pool);
                return new PreparedTransactionException(
                    format("[%s] Can't openForWrite '%s' using an already prepared transaction",
                        config.familyName, toDebugString(object)));
            case ABORTED:
                return new DeadTransactionException(
                    format("[%s] Can't openForWrite '%s' using an already aborted transaction",
                        config.familyName, toDebugString(object)));
            case COMMITTED:
                return new DeadTransactionException(
                    format("[%s] Can't openForWrite '%s' using an already committed transaction",
                        config.familyName, toDebugString(object)));
            default:
                throw new IllegalStateException();
        }
    }

    public final IllegalTransactionStateException abortOpenForConstruction(
        final BetaObjectPool pool, final BetaTransactionalObject object) {

        switch (status) {
            case PREPARED:
                abort(pool);
                return new PreparedTransactionException(
                    format("[%s] Can't openForConstruction '%s' using an already prepared transaction",
                        config.familyName, toDebugString(object)));
            case ABORTED:
                return new DeadTransactionException(
                    format("[%s] Can't openForConstruction '%s' using an already aborted transaction",
                        config.familyName, toDebugString(object)));
            case COMMITTED:
                return new DeadTransactionException(
                    format("[%s] Can't openForConstruction '%s' using an already committed transaction",
                        config.familyName, toDebugString(object)));
            default:
                throw new IllegalStateException();
        }
    }

    public final IllegalTransactionStateException abortCommute(
        final BetaObjectPool pool, final BetaTransactionalObject object, final Function function) {

        switch (status) {
           case PREPARED:
               abort(pool);
               return new PreparedTransactionException(
                    format("[%s] Can't commuting '%s' with reference '%s' using an already prepared transaction",
                        config.familyName, toDebugString(object), function));
           case ABORTED:
               return new DeadTransactionException(
                    format("[%s] Can't commuting '%s' with reference '%s' using an already aborted transaction",
                        config.familyName, toDebugString(object), function));
           case COMMITTED:
               return new DeadTransactionException(
                    format("[%s] Can't commuting '%s' with reference '%s' using an already committed transaction",
                        config.familyName, toDebugString(object), function));
           default:
               throw new IllegalStateException();
       }
    }

    @Override
    public final BetaTransactionConfiguration getConfiguration() {
        return config;
    }

    @Override
    public final int getAttempt() {
        return attempt;
    }

    @Override
    public final TransactionStatus getStatus() {
        switch (status) {
            case NEW:
                return TransactionStatus.Unstarted;
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
    public final long getRemainingTimeoutNs() {
        return remainingTimeoutNs;
    }

    @Override
    public final void setRemainingTimeoutNs(long timeoutNs) {
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

    /**
     * Prepares this BetaTransaction.
     *
     * @param pool the BetaObjectPool
     */
    public abstract void prepare(BetaObjectPool pool);

    /**
     * Commits this BetaTransaction.
     *
     * @param pool the BetaObjectPool for putting/taking poolable resources.
     * @throws ControlFlowError e.g. on read or write conflicts.
     * @throws DeadTransactionException if the transaction already is aborted.
     */
    public abstract void commit(BetaObjectPool pool);

    /**
     * Aborts this BetaTransaction.
     *
     * throws DeadTransactionException if the transaction already is committed.
     */
    public abstract void abort(BetaObjectPool pool);

    /**
     * Resets this BetaTransaction.
     *
     * @param pool the BetaObjectPool for putting/taking poolable resources.
     */
    public abstract boolean softReset(BetaObjectPool pool);

    /**
     *
     */
    public abstract void hardReset(BetaObjectPool pool);

    /**
     * Returns the tranlocal that belongs to the given transactional object.
     *
     * @returns the found tranlocal, or null if not found.
     */
    public abstract Tranlocal get(BetaTransactionalObject object);

    /**
     * Returns a list containing the permanent TransactionLifecycleListeners. The returned list
     * can be null (essentially the same as an empty list).
     */
    public abstract ArrayList<TransactionLifecycleListener> getPermanentListeners();

    /**
     * Returns a list containing the normal TransactionLifecycleListeners. The returned list
     * can be null (essentially the same as an empty list).
     *
     */
    public abstract ArrayList<TransactionLifecycleListener> getNormalListeners();

    public abstract void register(BetaObjectPool pool, TransactionLifecycleListener listener);

    public abstract void copyForSpeculativeFailure(BetaTransaction tx);

    /**
     *
     *
     */
    public abstract void init(BetaTransactionConfiguration transactionConfig, BetaObjectPool pool);

    /**
     * Registers the changeListener and aborts the transaction (so also releasing its acquired resources
     * like locks.
     */
    public abstract void registerChangeListenerAndAbort(Latch changeListener, BetaObjectPool pool);

    public abstract void startEitherBranch(BetaObjectPool pool);

    public abstract void endEitherBranch(BetaObjectPool pool);

    public abstract void startOrElseBranch(BetaObjectPool pool);

    public abstract void addWatch(BetaTransactionalObject object, Watch watch);

    public abstract <E> RefTranlocal<E> openForRead(Ref<E> ref, boolean lock, BetaObjectPool pool);

    public abstract <E> RefTranlocal<E> openForWrite(Ref<E> ref, boolean lock, BetaObjectPool pool);

    public abstract <E> RefTranlocal<E> openForConstruction(Ref<E> ref, BetaObjectPool pool);

    public abstract <E> void commute(Ref<E> ref, BetaObjectPool pool, Function<E> function);

    public abstract  IntRefTranlocal openForRead(IntRef ref, boolean lock, BetaObjectPool pool);

    public abstract  IntRefTranlocal openForWrite(IntRef ref, boolean lock, BetaObjectPool pool);

    public abstract  IntRefTranlocal openForConstruction(IntRef ref, BetaObjectPool pool);

    public abstract  void commute(IntRef ref, BetaObjectPool pool, IntFunction function);

    public abstract  LongRefTranlocal openForRead(LongRef ref, boolean lock, BetaObjectPool pool);

    public abstract  LongRefTranlocal openForWrite(LongRef ref, boolean lock, BetaObjectPool pool);

    public abstract  LongRefTranlocal openForConstruction(LongRef ref, BetaObjectPool pool);

    public abstract  void commute(LongRef ref, BetaObjectPool pool, LongFunction function);

    public abstract  Tranlocal openForRead(BetaTransactionalObject ref, boolean lock, BetaObjectPool pool);

    public abstract  Tranlocal openForWrite(BetaTransactionalObject ref, boolean lock, BetaObjectPool pool);

    public abstract  Tranlocal openForConstruction(BetaTransactionalObject ref, BetaObjectPool pool);

    public abstract  void commute(BetaTransactionalObject ref, BetaObjectPool pool, Function function);
}
