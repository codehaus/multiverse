package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.Listeners;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.api.programmatic.ProgrammaticRef;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import java.io.File;

import static java.lang.String.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.exceptions.UncommittedReadConflict.createUncommittedReadConflict;

/**
 * A manual instrumented {@link org.multiverse.transactional.refs.Ref} implementation. If this class
 * is used,you don't need to worry about instrumentation/javaagents and stuff like this.
 * <p/>
 * It is added to getClassMetadata the Akka project up and running, but probably will removed when the instrumentation is 100% up
 * and running and this can be done compiletime instead of messing with javaagents.
 * <h3>Lifting on a transaction</h3>
 * All methods automatically lift on a transaction if one is available, but to reduce the need for extra object
 * creation and unwanted ThreadLocal access, there also are methods available that have a
 * {@link org.multiverse.api.Transaction} as first argument.
 * <h3>TransactionFactory</h3>
 * All methods of this AlphaProgrammaticRef also have a version that accepts a {@link org.multiverse.api.TransactionFactory}. TransactionFactories
 * can be quite expensive to createReference, so it is best to createReference them up front and reuse them. TransactionFactories
 * are threadsafe to use, so no worries about that as well.
 * <h3>Performance</h3>
 * The AlphaProgrammaticRef already has been heavily optimized and prevents unwanted creation of objects like
 * Transactions or TransactionTemplates. If you really need more performance you should talk to me
 * about adding instrumentation.
 * <h3>Relying on GlobalStmInstance</h3>
 * This BasicRef implementation can be used without depending on the GlobalStmInstance (so you could createReference a local
 * one stm instance). If this is done, only the methods that rely on a Transaction or TransactionFactory
 * should be used.
 * <h3>___ methods</h3>
 * If you use code completion on this class you will also find ___ methods. These methods are not for you
 * unless you really know what you do. So ignore them.
 * <p/>
 * TODO:
 * The internal templates created here don't need to have lifecycle callbacks enabled.
 * <p/>
 * <p/>
 * Possible optimization for the alpha engine, instead of placing the lock, listener on the
 * transactional object, place it on the tranlocal. This would remove the writeLock after
 * commit because when the new tranlocal is written, the lock automatically is null.
 * <p/>
 * The listener can also be read from the tranlocal so no need to
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticRef<E>
        extends DefaultTxObjectMixin implements ProgrammaticRef<E> {

    //should only be used for testing purposes.

    public static AlphaProgrammaticRef createUncommitted() {
        return new AlphaProgrammaticRef((File) null);
    }

    private final static TransactionFactory getOrAwaitTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(true)
            .setFamilyName(AlphaProgrammaticRef.class.getName() + ".getOrAwait()")
            .setReadTrackingEnabled(true)
            .build();

    private final AlphaStm stm;

    /**
     * Creates a new BasicRef with null as value. It has exactly the same {@link #AlphaProgrammaticRef(Object)}
     * with null as value.
     * <p/>
     * This method relies on the ThreadLocalTransaction and GlobalStmInstance.
     */
    public AlphaProgrammaticRef() {
        this(getThreadLocalTransaction(), null);
    }

    private AlphaProgrammaticRef(File file) {
        this.stm = null;
    }

    /**
     * Creates a new BasicRef with the provided value.
     * <p/>
     * If there is no transaction active, the writeversion of the committed reference will
     * be the same as the current version of the stm. Normally it increases after a commit to
     * indicate that a change has been made. But for a transaction that only adds new transactional
     * objects instead of modifying them this isn't needed. This reduces stress on the
     * <p/>
     * This method relies on the ThreadLocalTransaction.
     * If no transaction is found, it also relies on the GlobalStmInstance.
     *
     * @param value the value this BasicRef should have.
     */
    public AlphaProgrammaticRef(E value) {
        this(getThreadLocalTransaction(), value);
    }

    /**
     * Creates a new BasicRef using the provided transaction.
     * <p/>
     * This method does not rely on a ThreadLocalTransaction and GlobalStmInstance.
     *
     * @param tx the Transaction used
     * @throws IllegalThreadStateException if the transaction was not in the correct state for
     *                                     creating this AlphaProgrammaticRef.
     */
    public AlphaProgrammaticRef(Transaction tx) {
        this(tx, null);
    }

    public AlphaProgrammaticRef(AlphaStm stm, E value) {
        if (stm == null) {
            throw new NullPointerException();
        }

        this.stm = stm;
        long writeVersion = this.stm.getVersion();
        AlphaProgrammaticRefTranlocal<E> tranlocal = new AlphaProgrammaticRefTranlocal<E>(this);
        tranlocal.value = value;
        ___storeInitial(tranlocal, writeVersion);
    }

    public AlphaProgrammaticRef(Transaction tx, E value) {
        //todo: this is not correct.
        stm = (AlphaStm) getGlobalStmInstance();

        AlphaTransaction alphaTx = (AlphaTransaction) tx;

        if (tx == null || tx.getStatus().isDead()) {
            long writeVersion = stm.getVersion();
            AlphaProgrammaticRefTranlocal<E> tranlocal = new AlphaProgrammaticRefTranlocal<E>(this);
            tranlocal.value = value;
            ___storeInitial(tranlocal, writeVersion);
            return;
        }

        AlphaProgrammaticRefTranlocal<E> tranlocal = (AlphaProgrammaticRefTranlocal<E>) alphaTx.openForConstruction(this);
        tranlocal.value = value;
    }

    private AlphaProgrammaticRefTranlocal<E> openForRead(Transaction tx) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        return (AlphaProgrammaticRefTranlocal<E>) alphaTx.openForRead(this);
    }

    private AlphaProgrammaticRefTranlocal<E> openForWrite(Transaction tx) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        return (AlphaProgrammaticRefTranlocal<E>) alphaTx.openForWrite(this);
    }

    // ============================== get ======================================

    @Override
    public E get() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return atomicGet();
        }

        return get(tx);
    }

    @Override
    public E get(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaProgrammaticRefTranlocal<E> tranlocal = openForRead(tx);
        return tranlocal.value;
    }

    @Override
    public E atomicGet() {
        AlphaProgrammaticRefTranlocal<E> tranlocal = (AlphaProgrammaticRefTranlocal) ___load();

        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        return tranlocal.value;
    }

    // ======================== isNull =======================================

    @Override
    public boolean isNull() {
        return get() == null;
    }

    @Override
    public boolean isNull(Transaction tx) {
        return get(tx) == null;
    }

    @Override
    public boolean atomicIsNull() {
        return atomicGet() == null;
    }

    // ============================== getOrAwait ======================================

    @Override
    public E getOrAwait() {
        return getOrAwait(getOrAwaitTxFactory);
    }

    @Override
    public E getOrAwait(TransactionFactory txFactory) {
        //todo: better parameter settings disabling lifecyclecallbacks
        return new TransactionTemplate<E>(txFactory) {
            @Override
            public E execute(Transaction t) {
                return getOrAwait(t);
            }
        }.execute();
    }

    @Override
    public E getOrAwait(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaProgrammaticRefTranlocal<E> tranlocal = openForRead(tx);

        if (tranlocal.value == null) {
            retry();
        }

        return tranlocal.value;
    }

    // ========================== set ==========================================


    @Override
    public E set(E newValue) {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return atomicSet(newValue);
        }

        return set(tx, newValue);
    }

    @Override
    public E set(Transaction tx, E newValue) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaProgrammaticRefTranlocal<E> readonly = openForRead(tx);

        //if there is no change, we are done.
        if (readonly.value == newValue) {
            return newValue;
        }

        AlphaProgrammaticRefTranlocal<E> tranlocal = openForWrite(tx);

        if (newValue == tranlocal.value) {
            return newValue;
        }

        E oldValue = tranlocal.value;
        tranlocal.value = newValue;
        return oldValue;
    }

    @Override
    public E atomicSet(E newValue) {
        AlphaProgrammaticRefTranlocal committed = (AlphaProgrammaticRefTranlocal) ___load();
        if (committed == null) {
            throw createUncommittedReadConflict();
        }

        if (committed.value == newValue) {
            return newValue;
        }

        AlphaProgrammaticRefTranlocal newTranlocal = new AlphaProgrammaticRefTranlocal(this);
        newTranlocal.value = newValue;

        //the AlphaProgrammaticRefTranlocal also implements the Transaction interface to prevent us
        //creating an additional objects even though we need an instance.
        Transaction lockOwner = newTranlocal;

        lock(lockOwner);


        AlphaProgrammaticRefTranlocal<E> oldTranlocal = (AlphaProgrammaticRefTranlocal<E>) ___load();

        long writeVersion = stm.getClock().tick();
        Listeners listeners = ___storeUpdate(newTranlocal, writeVersion, true);
        if (listeners != null) {
            listeners.openAll();
        }
        return oldTranlocal.value;
    }

    private void lock(Transaction lockOwner) {
        //if we couldn't acquire the lock, we are done.
        for (int attempt = 0; attempt <= stm.getMaxRetries(); attempt++) {
            lockOwner.setAttempt(attempt);
            if (attempt == stm.getMaxRetries()) {
                throw new TooManyRetriesException();
            } else if (___tryLock(lockOwner)) {
                return;
            } else {
                stm.getBackoffPolicy().delayedUninterruptible(lockOwner);
            }
        }
    }

    @Override
    public boolean atomicCompareAndSet(E expected, E update) {
        AlphaProgrammaticRefTranlocal<E> committed = (AlphaProgrammaticRefTranlocal<E>) ___load();

        if (committed == null) {
            throw new UncommittedReadConflict();
        }

        if (committed.value != expected) {
            return false;
        }

        if (committed.value == update) {
            return true;
        }

        AlphaProgrammaticRefTranlocal updateTranlocal = new AlphaProgrammaticRefTranlocal(this);
        updateTranlocal.value = update;

        Transaction lockOwner = updateTranlocal;
        if (!___tryLock(lockOwner)) {
            return false;
        }

        AlphaProgrammaticRefTranlocal<E> oldTranlocal = (AlphaProgrammaticRefTranlocal<E>) ___load();

        if (oldTranlocal.value != expected) {
            ___releaseLock(lockOwner);
            return false;
        }

        long writeVersion = stm.getClock().tick();
        Listeners listeners = ___storeUpdate(updateTranlocal, writeVersion, true);
        if (listeners != null) {
            listeners.openAll();
        }
        return true;
    }

    // ======================= toString =============================================

    @Override
    public String toString() {
        E value = get();
        return toString(value);
    }

    @Override
    public String toString(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaProgrammaticRefTranlocal<E> tranlocal = openForRead(tx);
        return toString(tranlocal.value);
    }

    private String toString(E value) {
        if (value == null) {
            return "AlphaProgrammaticRef(reference=null)";
        } else {
            return format("AlphaProgrammaticRef(reference=%s)", value);
        }
    }

    @Override
    public AlphaProgrammaticRefTranlocal<E> ___openUnconstructed() {
        return new AlphaProgrammaticRefTranlocal<E>(this);
    }
}
