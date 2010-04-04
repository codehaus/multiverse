package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.Listeners;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.api.programmatic.ProgrammaticReference;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import static java.lang.String.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.exceptions.CommitLockNotFreeWriteConflict.createFailedToObtainCommitLocksException;

/**
 * A manual instrumented {@link org.multiverse.transactional.TransactionalReference} implementation. If this class
 * is used,you don't need to worry about instrumentation/javaagents and stuff like this.
 * <p/>
 * It is added to getClassMetadata the Akka project up and running, but probably will removed when the instrumentation is 100% up
 * and running and this can be done compiletime instead of messing with javaagents.
 * <h3>Lifting on a transaction</h3>
 * All methods automatically lift on a transaction if one is available, but to reduce the need for extra object
 * creation and unwanted ThreadLocal access, there also are methods available that have a
 * {@link org.multiverse.api.Transaction} as first argument.
 * <h3>TransactionFactory</h3>
 * All methods of this AlphaProgrammaticReference also have a version that accepts a {@link org.multiverse.api.TransactionFactory}. TransactionFactories
 * can be quite expensive to createReference, so it is best to createReference them up front and reuse them. TransactionFactories
 * are threadsafe to use, so no worries about that as well.
 * <h3>Performance</h3>
 * The AlphaProgrammaticReference already has been heavily optimized and prevents unwanted creation of objects like
 * Transactions or TransactionTemplates. If you really need more performance you should talk to me
 * about adding instrumentation.
 * <h3>Relying on GlobalStmInstance</h3>
 * This Ref implementation can be used without depending on the GlobalStmInstance (so you could createReference a local
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
public final class AlphaProgrammaticReference<E> extends DefaultTxObjectMixin implements ProgrammaticReference<E> {

    private final static TransactionFactory getOrAwaitTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(true)
            .setFamilyName(AlphaProgrammaticReference.class.getName() + ".getOrAwait()")
            .setAutomaticReadTracking(true)
            .build();

    private static final PrimitiveClock clock = ((AlphaStm) getGlobalStmInstance()).getClock();

    /**
     * Creates a new Ref with null as value. It has exactly the same {@link #AlphaProgrammaticReference(Object)}
     * with null as value.
     * <p/>
     * This method relies on the ThreadLocalTransaction and GlobalStmInstance.
     */
    public AlphaProgrammaticReference() {
        this(getThreadLocalTransaction(), null);
    }

    /**
     * Creates a new Ref with the provided value.
     * <p/>
     * If there is no transaction active, the writeversion of the committed reference will
     * be the same as the current version of the stm. Normally it increases after a commit to
     * indicate that a change has been made. But for a transaction that only adds new transactional
     * objects instead of modifying them this isn't needed. This reduces stress on the
     * <p/>
     * This method relies on the ThreadLocalTransaction.
     * If no transaction is found, it also relies on the GlobalStmInstance.
     *
     * @param value the value this Ref should have.
     */
    public AlphaProgrammaticReference(E value) {
        this(getThreadLocalTransaction(), value);
    }

    /**
     * Creates a new Ref using the provided transaction.
     * <p/>
     * This method does not rely on a ThreadLocalTransaction and GlobalStmInstance.
     *
     * @param tx the Transaction used
     * @throws IllegalThreadStateException if the transaction was not in the correct state for
     *                                     creating this AlphaProgrammaticReference.
     */
    public AlphaProgrammaticReference(Transaction tx) {
        this(tx, null);
    }

    public AlphaProgrammaticReference(Transaction tx, E value) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;

        if (tx == null || tx.getStatus().isDead()) {
            long writeVersion = clock.getVersion();
            AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = new AlphaProgrammaticRefeferenceTranlocal<E>(this);
            tranlocal.value = value;
            ___storeInitial(tranlocal, writeVersion);
            return;
        }

        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = (AlphaProgrammaticRefeferenceTranlocal<E>) alphaTx.openForConstruction(this);
        tranlocal.value = value;
    }

    private AlphaProgrammaticRefeferenceTranlocal<E> openForRead(Transaction tx) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        return (AlphaProgrammaticRefeferenceTranlocal<E>) alphaTx.openForRead(this);
    }

    private AlphaProgrammaticRefeferenceTranlocal<E> openForWrite(Transaction tx) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        return (AlphaProgrammaticRefeferenceTranlocal<E>) alphaTx.openForWrite(this);
    }

    // ============================== getVersion ===============================


    @Override
    public long getVersion() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return atomicGetVersion();
        }

        return getVersion(tx);
    }

    @Override
    public long getVersion(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = openForRead(tx);
        return tranlocal.___writeVersion;
    }

    @Override
    public long atomicGetVersion() {
        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = (AlphaProgrammaticRefeferenceTranlocal<E>) ___load();

        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        return tranlocal.___writeVersion;
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

        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = openForRead(tx);
        return tranlocal.value;
    }

    @Override
    public E atomicGet() {
        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = (AlphaProgrammaticRefeferenceTranlocal) ___load();

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
        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = openForRead(tx);
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

        AlphaProgrammaticRefeferenceTranlocal<E> readonly = openForRead(tx);

        //if there is no change, we are done.
        if (readonly.value == newValue) {
            return newValue;
        }

        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = openForWrite(tx);

        if (newValue == tranlocal.value) {
            return newValue;
        }

        E oldValue = tranlocal.value;
        tranlocal.value = newValue;
        return oldValue;
    }

    @Override
    public E atomicSet(E newValue) {
        //if there is no difference we are done
        E oldValue = atomicGet();

        if (oldValue == newValue) {
            return oldValue;
        }

        AlphaProgrammaticRefeferenceTranlocal newTranlocal = new AlphaProgrammaticRefeferenceTranlocal(this);
        newTranlocal.value = newValue;

        //the AlphaProgrammaticRefeferenceTranlocal also implements the Transaction interface to prevent us
        //creating an additional objects even though we need an instance.
        Transaction tx = newTranlocal;

        //if we couldn't acquire the lock, we are done.
        if (!___tryLock(tx)) {
            throw createFailedToObtainCommitLocksException();
        }

        AlphaProgrammaticRefeferenceTranlocal<E> oldTranlocal = (AlphaProgrammaticRefeferenceTranlocal<E>) ___load();

        long writeVersion = clock.tick();
        ___storeUpdate(newTranlocal, writeVersion, true);
        return oldTranlocal.value;
    }

    @Override
    public boolean atomicCompareAndSet(E expected, E update) {
        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = (AlphaProgrammaticRefeferenceTranlocal<E>) ___load();

        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        if (tranlocal.value != expected) {
            return false;
        }

        AlphaProgrammaticRefeferenceTranlocal newTranlocal = new AlphaProgrammaticRefeferenceTranlocal(this);
        Transaction lockOwner = (Transaction) newTranlocal;
        newTranlocal.value = update;

        //if we couldn't acquire the lock, we are done.
        if (!___tryLock(lockOwner)) {
            throw createFailedToObtainCommitLocksException();
        }

        AlphaProgrammaticRefeferenceTranlocal<E> oldTranlocal = (AlphaProgrammaticRefeferenceTranlocal<E>) ___load();

        if (oldTranlocal.value != expected) {
            ___releaseLock(lockOwner);
            return false;
        }

        long writeVersion = clock.tick();
        Listeners listeners = ___storeUpdate(newTranlocal, writeVersion, true);
        if (listeners != null) {
            listeners.openAll();
        }
        return true;
    }

    // ======================== clear ========================================

    @Override
    public E clear() {
        return set(null);
    }

    @Override
    public E clear(Transaction tx) {
        return set(tx, null);
    }

    @Override
    public E atomicClear() {
        return atomicSet(null);
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

        AlphaProgrammaticRefeferenceTranlocal<E> tranlocal = openForRead(tx);
        return toString(tranlocal.value);
    }

    private String toString(E value) {
        if (value == null) {
            return "DefaultTransactionalReference(reference=null)";
        } else {
            return format("DefaultTransactionalReference(reference=%s)", value);
        }
    }

    @Override
    public AlphaProgrammaticRefeferenceTranlocal<E> ___openUnconstructed() {
        return new AlphaProgrammaticRefeferenceTranlocal<E>(this);
    }
}
