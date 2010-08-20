package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.ControlFlowError;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.functions.Function;
import org.multiverse.functions.IntFunction;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.transactionalobjects.*;

import java.util.ArrayList;

/**
 * @author Peter Veentjer
 */
public interface BetaTransaction extends Transaction {

    int POOL_TRANSACTIONTYPE_LEAN_MONO = 0;
    int POOL_TRANSACTIONTYPE_FAT_MONO = 1;
    int POOL_TRANSACTIONTYPE_LEAN_ARRAY = 2;
    int POOL_TRANSACTIONTYPE_FAT_ARRAY = 3;
    int POOL_TRANSACTIONTYPE_LEAN_ARRAYTREE = 4;
    int POOL_TRANSACTIONTYPE_FAT_ARRAYTREE = 5;

    int getPoolTransactionType();

    void start(BetaObjectPool pool);

    /**
     * Prepares this BetaTransaction.
     *
     * @param pool the BetaObjectPool
     */
    void prepare(BetaObjectPool pool);

    /**
     * Commits this BetaTransaction.
     *
     * @param pool the BetaObjectPool for putting/taking poolable resources.
     * @throws ControlFlowError e.g. on read or write conflicts.
     * @throws DeadTransactionException if the transaction already is aborted.
     */
    void commit(BetaObjectPool pool);

    /**
     * Aborts this BetaTransaction.
     *
     * throws DeadTransactionException if the transaction already is committed.
     */
    void abort(BetaObjectPool pool);

    /**
     * Resets this BetaTransaction.
     *
     * @param pool the BetaObjectPool for putting/taking poolable resources.
     */
    boolean softReset(BetaObjectPool pool);

    /**
     *
     */
    void hardReset(BetaObjectPool pool);

    /**
     * Returns the tranlocal that belongs to the given transactional object.
     *
     * @returns the found tranlocal, or null if not found.
     */
    Tranlocal get(BetaTransactionalObject object);

    /**
     * Returns a list containing the permanent TransactionLifecycleListeners. The returned list
     * can be null (essentially the same as an empty list).
     */
    ArrayList<TransactionLifecycleListener> getPermanentListeners();

    /**
     * Returns a list containing the normal TransactionLifecycleListeners. The returned list
     * can be null (essentially the same as an empty list).
     *
     */
    ArrayList<TransactionLifecycleListener> getNormalListeners();

    void register(BetaObjectPool pool, TransactionLifecycleListener listener);

    void copyForSpeculativeFailure(BetaTransaction tx);

    @Override
    BetaTransactionConfiguration getConfiguration();

    /**
     *
     *
     */
    void init(BetaTransactionConfiguration transactionConfig, BetaObjectPool pool);

    /**
     * Registers the changelistener and aborts the transaction (so also releasing its acquired resources
     * like locks.
     */
    void registerChangeListenerAndAbort(Latch changeListener, BetaObjectPool pool);

    void startEitherBranch(BetaObjectPool pool);

    void endEitherBranch(BetaObjectPool pool);

    void startOrElseBranch(BetaObjectPool pool);

    <E> RefTranlocal<E> openForRead(Ref<E> ref, boolean lock, BetaObjectPool pool);

    <E> RefTranlocal<E> openForWrite(Ref<E> ref, boolean lock, BetaObjectPool pool);

    <E> RefTranlocal<E> openForConstruction(Ref<E> ref, BetaObjectPool pool);

    <E> void commute(Ref<E> ref, BetaObjectPool pool, Function<E> function);

     IntRefTranlocal openForRead(IntRef ref, boolean lock, BetaObjectPool pool);

     IntRefTranlocal openForWrite(IntRef ref, boolean lock, BetaObjectPool pool);

     IntRefTranlocal openForConstruction(IntRef ref, BetaObjectPool pool);

     void commute(IntRef ref, BetaObjectPool pool, IntFunction function);

     LongRefTranlocal openForRead(LongRef ref, boolean lock, BetaObjectPool pool);

     LongRefTranlocal openForWrite(LongRef ref, boolean lock, BetaObjectPool pool);

     LongRefTranlocal openForConstruction(LongRef ref, BetaObjectPool pool);

     void commute(LongRef ref, BetaObjectPool pool, LongFunction function);

     Tranlocal openForRead(BetaTransactionalObject ref, boolean lock, BetaObjectPool pool);

     Tranlocal openForWrite(BetaTransactionalObject ref, boolean lock, BetaObjectPool pool);

     Tranlocal openForConstruction(BetaTransactionalObject ref, BetaObjectPool pool);

     void commute(BetaTransactionalObject ref, BetaObjectPool pool, Function function);
}
