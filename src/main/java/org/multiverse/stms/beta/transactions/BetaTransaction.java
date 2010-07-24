package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.Tranlocal;
import org.multiverse.stms.beta.refs.*;

/**
 * @author Peter Veentjer
 */
public interface BetaTransaction extends Transaction {

    void start(ObjectPool pool);

    /**
     * Prepares this BetaTransaction.
     *
     *
     * @param the ObjectPool
     */
    void prepare(ObjectPool pool);

    /**
     * Commits this BetaTransaction.
     *
     * @param pool the ObjectPool for putting/taking poolable resources.
     * @throws ControlFlowError e.g. on read or write conflicts.
     * @throws DeadTransactionException if the transaction already is aborted.
     */
    void commit(ObjectPool pool);

    /**
     * Aborts this BetaTransaction.
     *
     * throws DeadTransactionException if the transaction already is committed.
     */
    void abort(ObjectPool pool);

    /**
     * Resets this BetaTransaction.
     *
     * @param pool the ObjectPool for putting/taking poolable resources.
     */
    void reset(ObjectPool pool);

    BetaTransactionConfig getConfig();

    /**
     *
     *
     */
    void init(BetaTransactionConfig transactionConfig, ObjectPool pool);
    
    /**
     * Registers the changelistener and aborts the transaction (so also releasing its acquired resources
     * like locks.
     */
    void registerChangeListenerAndAbort(Latch changeListener, ObjectPool pool);

    void startEitherBranch(ObjectPool pool);

    void endEitherBranch(ObjectPool pool);

    void startOrElseBranch(ObjectPool pool);

    <E> RefTranlocal<E>  openForRead(Ref<E> ref, boolean lock, ObjectPool pool);

    <E> RefTranlocal<E>  openForWrite(Ref<E> ref, boolean lock, ObjectPool pool);

    <E> RefTranlocal<E>  openForConstruction(Ref<E> ref, ObjectPool pool);
     IntRefTranlocal  openForRead(IntRef ref, boolean lock, ObjectPool pool);

     IntRefTranlocal  openForWrite(IntRef ref, boolean lock, ObjectPool pool);

     IntRefTranlocal  openForConstruction(IntRef ref, ObjectPool pool);
     LongRefTranlocal  openForRead(LongRef ref, boolean lock, ObjectPool pool);

     LongRefTranlocal  openForWrite(LongRef ref, boolean lock, ObjectPool pool);

     LongRefTranlocal  openForConstruction(LongRef ref, ObjectPool pool);
     DoubleRefTranlocal  openForRead(DoubleRef ref, boolean lock, ObjectPool pool);

     DoubleRefTranlocal  openForWrite(DoubleRef ref, boolean lock, ObjectPool pool);

     DoubleRefTranlocal  openForConstruction(DoubleRef ref, ObjectPool pool);
     Tranlocal  openForRead(BetaTransactionalObject ref, boolean lock, ObjectPool pool);

     Tranlocal  openForWrite(BetaTransactionalObject ref, boolean lock, ObjectPool pool);

     Tranlocal  openForConstruction(BetaTransactionalObject ref, ObjectPool pool);
}
