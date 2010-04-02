package org.multiverse.stms.alpha;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.utils.clock.PrimitiveClock;

import static java.lang.String.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.exceptions.CommitLockNotFreeWriteConflict.newFailedToObtainCommitLocksException;

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
 * All methods of this AlphaProgrammaticReference also have a version that accepts a {@link TransactionFactory}. TransactionFactories
 * can be quite expensive to create, so it is best to create them up front and reuse them. TransactionFactories
 * are threadsafe to use, so no worries about that as well.
 * <h3>Performance</h3>
 * The AlphaProgrammaticReference already has been heavily optimized and prevents unwanted creation of objects like
 * Transactions or TransactionTemplates. If you really need more performance you should talk to me
 * about adding instrumentation.
 * <h3>Relying on GlobalStmInstance</h3>
 * This Ref implementation can be used without depending on the GlobalStmInstance (so you could create a local
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
 * Piossible optimization for the alpha engine, instead of placing the lock, listener on the
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
        this((E) null);
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
        Transaction tx = getThreadLocalTransaction();
        if (tx == null || tx.getStatus().isDead()) {
            long writeVersion = clock.getVersion();
            AlphaRefTranlocal<E> tranlocal = new AlphaRefTranlocal<E>(this);
            tranlocal.value = value;
            ___storeInitial(tranlocal, writeVersion);
        } else {
            AlphaTransaction alphaTx = (AlphaTransaction) tx;
            AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal<E>) alphaTx.openForConstruction(this);
            tranlocal.value = value;
        }
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
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal<E>) alphaTx.openForConstruction(this);
        tranlocal.value = value;
    }

    private AlphaRefTranlocal<E> openForRead(Transaction tx) {
        return (AlphaRefTranlocal<E>) ((AlphaTransaction) tx).openForRead(AlphaProgrammaticReference.this);
    }

    private AlphaRefTranlocal<E> openForWrite(Transaction tx) {
        return (AlphaRefTranlocal<E>) ((AlphaTransaction) tx).openForWrite(AlphaProgrammaticReference.this);
    }

    // ============================== getVersion ===============================

    @Override
    public long getVersionAtomic() {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal<E>) ___load();

        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        return tranlocal.___writeVersion;
    }

    @Override
    public long getVersion() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return getVersionAtomic();
        } else {
            return getVersion(tx);
        }
    }

    @Override
    public long getVersion(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = openForRead(tx);
        return tranlocal.___writeVersion;
    }

    // ============================== get ======================================

    @Override
    public E get() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return getAtomic();
        } else {
            return get(tx);
        }
    }

    @Override
    public E getAtomic() {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal) ___load();

        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }
        return tranlocal.value;
    }

    @Override
    public E get(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = openForRead(tx);
        return tranlocal.value;
    }

    // ======================== isNull =======================================

    @Override
    public boolean isNullAtomic() {
        return getAtomic() == null;
    }

    @Override
    public boolean isNull() {
        return get() == null;
    }

    @Override
    public boolean isNull(Transaction tx) {
        return get(tx) == null;
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
        AlphaRefTranlocal<E> tranlocal = openForRead(tx);
        if (tranlocal.value == null) {
            retry();
        }

        return tranlocal.value;
    }

    // ========================== set ==========================================

    @Override
    public E setAtomic(E newValue) {
        //if there is no difference we are done
        E oldValue = getAtomic();

        if (oldValue == newValue) {
            return oldValue;
        }

        AlphaRefTranlocal newTranlocal = new AlphaRefTranlocal(this);
        newTranlocal.value = newValue;

        //the AlphaRefTranlocal also implements the Transaction interface to prevent us
        //creating an additional objects even though we need an instance.
        Transaction tx = newTranlocal;

        //if we couldn't acquire the lock, we are done.
        if (!___tryLock(tx)) {
            throw newFailedToObtainCommitLocksException();
        }

        AlphaRefTranlocal<E> oldTranlocal = (AlphaRefTranlocal<E>) ___load();

        long writeVersion = clock.tick();
        ___storeUpdate(newTranlocal, writeVersion, true);
        return oldTranlocal.value;
    }

    @Override
    public E set(E newValue) {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return setAtomic(newValue);
        } else {
            return set(tx, newValue);
        }
    }

    @Override
    public E set(Transaction tx, E newValue) {
        AlphaRefTranlocal<E> readonly = openForRead(tx);

        //if there is no change, we are done.
        if (readonly.value == newValue) {
            return newValue;
        }

        AlphaRefTranlocal<E> tranlocal = openForWrite(tx);

        if (newValue == tranlocal.value) {
            return newValue;
        }

        E oldValue = tranlocal.value;
        tranlocal.value = newValue;
        return oldValue;
    }


    @Override
    public E setAtomic(E newValue, long expectedVersion) {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal<E>) ___load();
        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        if (tranlocal.value == newValue) {
            return newValue;
        }

        if (tranlocal.getWriteVersion() != expectedVersion) {
            throw new OptimisticLockingFailureException();
        }

        AlphaRefTranlocal newTranlocal = new AlphaRefTranlocal(this);
        newTranlocal.value = newValue;

        //the AlphaRefTranlocal also implements the Transaction interface to prevent us
        //creating an additional objects even though we need an instance.
        Transaction tx = newTranlocal;

        //if we couldn't acquire the lock, we are done.
        if (!___tryLock(tx)) {
            throw newFailedToObtainCommitLocksException();
        }

        AlphaRefTranlocal<E> oldTranlocal = (AlphaRefTranlocal<E>) ___load();

        long writeVersion = clock.tick();
        ___storeUpdate(newTranlocal, writeVersion, true);
        return oldTranlocal.value;
    }

    // ======================== clear ========================================

    @Override
    public E clearAtomic() {
        return setAtomic(null);
    }

    @Override
    public E clear() {
        return set(null);
    }

    @Override
    public E clear(Transaction tx) {
        return set(tx, null);
    }

    // ======================= toString =============================================

    @Override
    public String toString() {
        E value = get();
        return toString(value);
    }

    @Override
    public String toString(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = openForRead(tx);
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
    public AlphaRefTranlocal<E> ___openUnconstructed() {
        return new AlphaRefTranlocal<E>(this);
    }
}


/**
 * The AlphaTranlocal for the AlphaProgrammaticReference. It is responsible for storing the state of the AlphaProgrammaticReference.
 * <p/>
 * The AlpaRefTranlocal also implements the Transaction interface because it can be
 * used as a lockOwner. This is done as a performance optimization.
 *
 * @param <E>
 */
class AlphaRefTranlocal<E> extends AlphaTranlocal implements Transaction {

    E value;

    AlphaRefTranlocal(AlphaRefTranlocal<E> origin) {
        this.___origin = origin;
        this.___transactionalObject = origin.___transactionalObject;
        this.value = origin.value;
    }

    AlphaRefTranlocal(AlphaProgrammaticReference<E> owner) {
        this(owner, null);
    }

    AlphaRefTranlocal(AlphaProgrammaticReference<E> owner, E value) {
        this.___transactionalObject = owner;
        this.value = value;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new AlphaRefTranlocal(this);
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new AlphaRefTranlocalSnapshot<E>(this);
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        AlphaRefTranlocal origin = (AlphaRefTranlocal) ___origin;
        if (origin.value != this.value) {
            return true;
        }

        return false;
    }

    @Override
    public void abort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransactionConfiguration getConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getReadVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransactionStatus getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void prepare() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restart() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerRetryLatch(Latch latch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerLifecycleListener(TransactionLifecycleListener listener) {
        throw new UnsupportedOperationException();
    }
}

class AlphaRefTranlocalSnapshot<E> extends AlphaTranlocalSnapshot {

    final AlphaRefTranlocal ___tranlocal;
    final E value;

    AlphaRefTranlocalSnapshot(AlphaRefTranlocal<E> tranlocal) {
        this.___tranlocal = tranlocal;
        this.value = tranlocal.value;
    }

    @Override
    public AlphaTranlocal getTranlocal() {
        return ___tranlocal;
    }

    @Override
    public void restore() {
        ___tranlocal.value = value;
    }
}

