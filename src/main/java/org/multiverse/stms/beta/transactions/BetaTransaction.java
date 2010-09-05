package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.TransactionalObject;
import org.multiverse.api.Watch;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.Function;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.*;

import java.util.ArrayList;

import static java.lang.String.format;
import static org.multiverse.stms.beta.BetaStmUtils.toDebugString;

/**
 * @author Peter Veentjer
 */
public abstract class BetaTransaction implements Transaction, BetaStmConstants {

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
    protected final BetaObjectPool pool = new BetaObjectPool();

    public BetaTransaction(int poolTransactionType, BetaTransactionConfiguration config) {
        this.poolTransactionType = poolTransactionType;
        this.config = config;
    }

    public final BetaObjectPool getPool(){
        return pool;
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

    public final NullPointerException abortCommuteOnNullFunction(final TransactionalObject ref) {
        abort();
        throw new NullPointerException(
            format("[%s] Commute function can't be null",
                config.familyName));
    }

    public final NoRetryPossibleException abortOnNoRetryPossible(){
        abort();
        throw new NoRetryPossibleException(
            format("[%s] Can't block transaction since there are no tracked reads",
                config.familyName));
    }

    public final NoRetryPossibleException abortOnNoBlockingAllowed(){
        abort();
        return new NoRetryPossibleException(
            format("[%s] Can't block transaction since it doesn't allow blocking",
                config.familyName));
    }

    public final IllegalTransactionStateException abortOnFaultyStatusOfRegisterChangeListenerAndAbort(){
        switch (status) {
            case PREPARED:
                abort();
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

    public final RuntimeException abortOnOpenForReadWhileEvaluatingCommute(
        final BetaTransactionalObject ref){

        abort();
        throw new IllegalTransactionStateException(
            format("[%s] Can't openForRead '%s' while evaluating a commuting function",
                config.familyName, toDebugString(ref)));
    }

    public final RuntimeException abortOnOpenForWriteWhileEvaluatingCommute(
        final BetaTransactionalObject ref){

        abort();
        throw new IllegalTransactionStateException(
            format("[%s] Can't openForWrite '%s' while evaluating a commuting function",
                config.familyName, toDebugString(ref)));
    }

    public final RuntimeException abortOnOpenForConstructionWhileEvaluatingCommute(
        final BetaTransactionalObject ref){

        abort();
        throw new IllegalTransactionStateException(
            format("[%s] Can't openForConstruction '%s' while evaluating a commuting function",
            config.familyName, toDebugString(ref)));
    }

    public final RuntimeException abortOnCommuteWhileEvaluatingCommute(
        BetaTransactionalObject ref){

        abort();
        throw new IllegalTransactionStateException(
            format("[%s] Can't add a commuting function to '%s' while evaluating a commuting function",
                config.familyName, toDebugString(ref)));
    }

    public final IllegalArgumentException abortOpenForConstructionWithBadReference(
        final BetaTransactionalObject ref){

        abort();
        return new IllegalArgumentException(
            format("[%s] Can't openForConstruction a previous committed object or an object '%s'",
                config.familyName, toDebugString(ref)));
    }

    public final ReadonlyException abortOpenForWriteWhenReadonly(
        final BetaTransactionalObject object){

        abort();
        return new ReadonlyException(
            format("[%s] Can't openForWrite '%s' on a readonly transaction",
                config.familyName, toDebugString(object)));
    }

    public final NullPointerException abortOpenForWriteWhenNullReference(){
        abort();
        return new NullPointerException(
            format("[%s] Can't openForWrite a null reference",
                config.familyName));
    }

    public final NullPointerException abortOpenForConstructionWhenNullReference(){
        abort();
        return new NullPointerException(
            format("[%s] Can't openForConstruction a null reference",
                config.familyName));
    }

    public final NullPointerException abortCommuteWhenNullReference(
        final Function function){

        abort();
        return new NullPointerException(
            format("[%s] Can't commute with a null reference and function '%s'",
                config.familyName, function));
    }

    public final ReadonlyException abortOpenForConstructionWhenReadonly(
        final BetaTransactionalObject object){

        abort();
        return new ReadonlyException(
            format("[%s] Can't openForConstruction '%s' using a readonly transaction",
                config.familyName, toDebugString(object)));
    }

    public final ReadonlyException abortCommuteWhenReadonly(
            final BetaTransactionalObject object, final Function function){

        abort();
        return new ReadonlyException(
            format("[%s] Can't commute on '%s' with function '%s' and a readonly transaction ''",
                 config.familyName, toDebugString(object), function));
    }

    public final IllegalTransactionStateException abortOpenForRead(final BetaTransactionalObject object) {
        switch (status) {
            case PREPARED:
                abort();
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
        final BetaTransactionalObject object) {

        switch (status) {
            case PREPARED:
                abort();
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
        final BetaTransactionalObject object) {

        switch (status) {
            case PREPARED:
                abort();
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
        final BetaTransactionalObject object, final Function function) {

        switch (status) {
           case PREPARED:
               abort();
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

    protected final ReadConflict abortOnReadConflict() {
        abort();
        return ReadConflict.INSTANCE;
    }

    protected final SpeculativeConfigurationError abortOnTooSmallSize(int minimalSize) {
        config.needsMinimalTransactionLength(minimalSize);
        abort();
        return SpeculativeConfigurationError.INSTANCE;
    }

    protected final WriteConflict abortOnWriteConflict() {
        abort();
        return WriteConflict.INSTANCE;
    }

    /**
     * Returns the tranlocal that belongs to the given transactional object.
     *
     * @returns the found tranlocal, or null if not found.
     */
    public abstract Tranlocal get(BetaTransactionalObject object);

    /**
     * Returns a list containing the normal TransactionLifecycleListeners. The returned list
     * can be null (essentially the same as an empty list).
     *
     */
    public abstract ArrayList<TransactionLifecycleListener> getNormalListeners();

    public abstract void copyForSpeculativeFailure(BetaTransaction tx);

    /**
     *
     *
     */
    public abstract void init(BetaTransactionConfiguration transactionConfig);


    public abstract void addWatch(BetaTransactionalObject object, Watch watch);

    public abstract <E> RefTranlocal<E> openForRead(BetaRef<E> ref, boolean lock);

    public abstract <E> RefTranlocal<E> openForWrite(BetaRef<E> ref, boolean lock);

    public abstract <E> RefTranlocal<E> openForConstruction(BetaRef<E> ref);

    public abstract <E> void commute(BetaRef<E> ref, final Function<E> function);

    public abstract  IntRefTranlocal openForRead(BetaIntRef ref, boolean lock);

    public abstract  IntRefTranlocal openForWrite(BetaIntRef ref, boolean lock);

    public abstract  IntRefTranlocal openForConstruction(BetaIntRef ref);

    public abstract  void commute(BetaIntRef ref, final IntFunction function);

    public abstract  LongRefTranlocal openForRead(BetaLongRef ref, boolean lock);

    public abstract  LongRefTranlocal openForWrite(BetaLongRef ref, boolean lock);

    public abstract  LongRefTranlocal openForConstruction(BetaLongRef ref);

    public abstract  void commute(BetaLongRef ref, final LongFunction function);

    public abstract  Tranlocal openForRead(BetaTransactionalObject ref, boolean lock);

    public abstract  Tranlocal openForWrite(BetaTransactionalObject ref, boolean lock);

    public abstract  Tranlocal openForConstruction(BetaTransactionalObject ref);

    public abstract  void commute(BetaTransactionalObject ref, final Function function);
}
