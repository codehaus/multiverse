package org.multiverse.stms.alpha;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.latches.Latch;

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
 * All methods of this AlphaRef also have a version that accepts a {@link TransactionFactory}. TransactionFactories
 * can be quite expensive to create, so it is best to create them up front and reuse them. TransactionFactories
 * are threadsafe to use, so no worries about that as well.
 * <h3>Performance</h3>
 * The AlphaRef already has been heavily optimized and prevents unwanted creation of objects like
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
public final class AlphaRef<E> extends DefaultTxObjectMixin {

    private final static TransactionFactory getOrAwaitTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(true)
            .setFamilyName(AlphaRef.class.getName() + ".getOrAwait()")
            .setSmartTxLengthSelector(true)
            .setAutomaticReadTracking(true).build();

    private static final String CREATE_COMMITTED_FAMILY_NAME = AlphaRef.class.getName() + ".createCommitted(Stm,E)";

    private static final PrimitiveClock clock = ((AlphaStm) getGlobalStmInstance()).getClock();

    /**
     * Creates a committed ref with a null value using the Stm in the {@link org.multiverse.api.GlobalStmInstance}.
     *
     * @return the created ref.
     * @see #createCommittedRef(org.multiverse.api.Stm , Object)
     */
    public static <E> AlphaRef<E> createCommittedRef() {
        return createCommittedRef(getGlobalStmInstance(), null);
    }

    /**
     * Creates a committed ref with a null value.
     *
     * @param stm the {@link org.multiverse.api.Stm} used for committing the ref.
     * @return the created ref.
     * @see #createCommittedRef(org.multiverse.api.Stm , Object)
     */
    public static <E> AlphaRef<E> createCommittedRef(Stm stm) {
        return createCommittedRef(stm, null);
    }

    /**
     * Creates a committed ref with the given value using the Stm in the {@link org.multiverse.api.GlobalStmInstance}.
     *
     * @param value the initial value of the DefaultTransactionalReference.
     * @return the created ref.
     * @see #createCommittedRef(org.multiverse.api.Stm, Object)
     */
    public static <E> AlphaRef<E> createCommittedRef(E value) {
        return createCommittedRef(getGlobalStmInstance(), value);
    }

    /**
     * Creates a committed ref with the given value and using the given Stm.
     * <p/>
     * This factory method should be called when one doesn't want to lift on the current transaction, but you want
     * something to be committed whatever happens. In the future behavior will be added propagation levels. But for the
     * time being this is the 'expect_new' implementation of this propagation level.
     * <p/>
     * If the value is an transactionalobject or has a reference to it (perhaps indirectly), and the transaction this
     * transactionalobject is created in is aborted (or hasn't committed) yet, you will getClassMetadata the dreaded {@link
     * org.multiverse.api.exceptions.UncommittedReadConflict}.
     *
     * @param stm   the {@link org.multiverse.api.Stm} used for committing the ref.
     * @param value the initial value of the ref. The value is allowed to be null.
     * @return the created ref.
     */
    public static <E> AlphaRef<E> createCommittedRef(Stm stm, E value) {
        Transaction tx = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setFamilyName(CREATE_COMMITTED_FAMILY_NAME)
                .setSmartTxLengthSelector(true)
                .build().start();
        AlphaRef<E> ref = new AlphaRef<E>(tx, value);
        tx.commit();
        return ref;
    }

    /**
     * Creates a new Ref with null as value. It has exactly the same {@link #AlphaRef(Object)}
     * with null as value.
     * <p/>
     * This method relies on the ThreadLocalTransaction and GlobalStmInstance.
     */
    public AlphaRef() {
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
    public AlphaRef(E value) {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            long writeVersion = clock.getVersion();
            AlphaRefTranlocal<E> tranlocal = new AlphaRefTranlocal<E>(this);
            tranlocal.value = value;
            ___store(tranlocal, writeVersion);
        } else {
            AlphaRefTranlocal<E> tranlocal = openForWrite(tx);
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
     *                                     creating this AlphaRef.
     */
    public AlphaRef(Transaction tx) {
        this(tx, null);
    }

    public AlphaRef(Transaction tx, E value) {
        AlphaRefTranlocal<E> tranlocal = openForWrite(tx);
        tranlocal.value = value;
    }

    private AlphaRefTranlocal<E> openForRead(Transaction tx) {
        return (AlphaRefTranlocal<E>) ((AlphaTransaction) tx).openForRead(AlphaRef.this);
    }

    private AlphaRefTranlocal<E> openForWrite(Transaction tx) {
        return (AlphaRefTranlocal<E>) ((AlphaTransaction) tx).openForWrite(AlphaRef.this);
    }

    // ============================== getVersion ===============================

    /**
     * Gets the version of the last committed tranlocal without looking at a transaction.
     * This call is very very fast since it doesn't need a transaction. See the {@link #getAtomic()}
     * for more information.
     * <p/>
     * The only expensive thing that needs to be done is a single volatile read. So it is in the same
     * league as a {@link java.util.concurrent.atomic.AtomicReference#get()}. To be more specific;
     * it would have the same performance as {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater#get(Object)}
     * since that is used under water.
     * <p/>
     * This functionality can be used for optimistic locking over multiple transactions.
     *
     * @return the version.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    public long getVersionAtomic() {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal<E>) ___load();

        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        return tranlocal.___writeVersion;
    }

    /**
     * Gets the version of the last committed tranlocal visible from the current transaction.
     * If no active transaction is found, the {@link #getVersionAtomic()} is called instead
     * (very very fast).
     * <p/>
     * If there is a using transaction, the transactional object will be added to the readset
     * if the transaction is configured to do that.
     * <p/>
     * This functionality can be used for optimistic locking over multiple transactions.
     *
     * @return the version.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    public long getVersion() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return getVersionAtomic();
        } else {
            return getVersion(tx);
        }
    }

    /**
     * Gets the version of the last committed tranlocal visible from the current transaction.
     * <p/>
     * So it could be that other transaction have committed after the tx is started, it will not
     * see these.
     * <p/>
     * This functionality can be used for optimistic locking over multiple transactions.
     *
     * @param tx the transaction used
     * @return the version
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction is not in the correct state for this operation.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    public long getVersion(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = openForRead(tx);
        return tranlocal.___writeVersion;
    }

    // ============================== get ======================================

    /**
     * Gets the value. If a Transaction already is running, it will lift on that transaction,
     * if not the {@link #getAtomic} is used. In that cases it will be very very cheap (roughly
     * 2/3 of the performance of {@link #getAtomic()}. The reason why this call is more expensive
     * than the {@link #getAtomic()} is that a getThreadlocalTransaction needs to be called and
     * a check on the Transaction if that is running.
     *
     * @return the current value stored in this reference.
     * @throws IllegalThreadStateException if the current transaction isn't in the right state
     *                                     for this operation.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *                                     if something fails while loading the reference.
     */
    public E get() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return getAtomic();
        } else {
            return get(tx);
        }
    }

    /**
     * Gets the value without looking at an existing transaction (it will run its 'own').
     * <p/>
     * This is a very cheap call since only one volatile read is needed an no additional
     * objects are created. On my machine I'm able to do 150.000.000 getAtomics per second
     * on a single thread to give some indication.
     * <p/>
     * The only expensive thing that needs to be done is a single volatile read. So it is in the same
     * league as a {@link java.util.concurrent.atomic.AtomicReference#get()}. To be more specific;
     * it would have the same performance as {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater#get(Object)}
     * since that is used under water.
     *
     * @return the current value.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    public E getAtomic() {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal) ___load();

        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }
        return tranlocal.value;
    }

    /**
     * Gets the value using the specified transaction.
     *
     * @param tx the Transaction used for reading the value.
     * @return the value currently stored, could be null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction is not
     *          in the correct state for this operation.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    public E get(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = openForRead(tx);
        return tranlocal.value;
    }

    // ======================== isNull =======================================

    /**
     * Checks if the value stored in this reference is null. This method doesn't lift on a
     * transaction, so it is very very cheap. See the {@link #getAtomic()} for more information.
     *
     * @return true if the reference is null, false otherwise.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    public boolean isNullAtomic() {
        return getAtomic() == null;
    }

    /**
     * Checks if the value stored in this reference is null.
     * <p/>
     * If a Transaction currently is active, it will lift on that transaction. If not, the
     * isNullAtomic is used, so very very cheap. See the {@link #get()} for more info since
     * that method is used to retrieve the current value.
     *
     * @return true if the reference currently is null.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    public boolean isNull() {
        return get() == null;
    }

    /**
     * Checks if the value stored in this reference is null using the provided transaction.
     *
     * @param tx the transaction used
     * @return true if the value is null, false otherwise.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                              is tx isn't active
     * @throws org.multiverse.api.exceptions.ReadConflict
     *                              if something fails while loading the reference.
     */
    public boolean isNull(Transaction tx) {
        return get(tx) == null;
    }

    // ============================== getOrAwait ======================================

    public E getOrAwait() {
        return getOrAwait(getOrAwaitTxFactory);
    }

    public E getOrAwait(TransactionFactory txFactory) {
        //todo: better parameter settings disabling lifecyclecallbacks
        return new TransactionTemplate<E>(txFactory) {
            @Override
            public E execute(Transaction t) throws Exception {
                return getOrAwait(t);
            }
        }.execute();
    }

    public E getOrAwait(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = openForRead(tx);
        if (tranlocal.value == null) {
            retry();
        }

        return tranlocal.value;
    }

    // ========================== set ==========================================

    /**
     * Sets the new value on this AlphaRef using its own transaction (so it doesn't
     * look at an existing transaction). This call is very fast (11M transactions/second
     * on my machine with a single thread.
     *
     * @param newValue the new value.
     * @return the old value.
     * @throws org.multiverse.api.exceptions.WriteConflict
     *          if something failed while committing. If the commit fails, nothing bad will happen.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
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
        try {
            ___store(newTranlocal, writeVersion);
        } finally {
            ___releaseLock(tx);
        }

        return oldTranlocal.value;
    }

    /**
     * Sets the new value on this reference. If an active transaction is available in the
     * ThreadLocalTransaction that will be used. If not, the {@link #setAtomic(Object)} is used
     * (which is very fast).
     *
     * @param newValue the new value to be stored in this reference. The newValue is allowed to
     *                 be null.
     * @return the value that is replaced, can be null.
     * @throws org.multiverse.api.exceptions.WriteConflict
     *                                     if something failed while committing. If the commit fails, nothing bad will happen.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *                                     if something fails while loading the reference.
     * @throws IllegalThreadStateException if the transaction was not in the correct state for
     *                                     this operations. If the transaction in the TransactionThreadLocal is dead (so aborted or
     *                                     committed), a new transaction will be used.
     */
    public E set(E newValue) {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return setAtomic(newValue);
        } else {
            return set(tx, newValue);
        }
    }

    /**
     * Sets the new value on this reference using the provided transaction.
     * <p/>
     * Use this call if you explicitly want to pass a transaction, instead of {@link #set(Object)}.
     *
     * @param tx       the transaction to use.
     * @param newValue the new value, and is allowed to be null.
     * @return the previous value stored in this reference.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction isn't in the correct state for this operation.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
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
        try {
            ___store(newTranlocal, writeVersion);
        } finally {
            ___releaseLock(tx);
        }

        return oldTranlocal.value;
    }

    // ======================== clear ========================================

    /**
     * Clears the reference using its own transaction without looking at an existing transaction.
     * It is the same as calling {@link #setAtomic(Object)} with a null value.
     *
     * @return the old value (can be null).
     * @throws org.multiverse.api.exceptions.WriteConflict
     *          if something failed while committing. If the commit fails, nothing bad will happen.
     */
    public E clearAtomic() {
        return setAtomic(null);
    }

    /**
     * Clears the reference. It is the same as calling {@link #set(Object)} with a null value.
     *
     * @return the previous value.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    public E clear() {
        return set(null);
    }

    /**
     * Clears the reference. It is the same as calling {@link #set(Transaction, Object)} with
     * a null value.
     *
     * @return the previous value.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction
     *          isn't in the correct state for this operation.
     */
    public E clear(Transaction tx) {
        return set(tx, null);
    }

    // ======================= toString =============================================

    @Override
    public String toString() {
        E value = get();
        return toString(value);
    }

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
 * The AlphaTranlocal for the AlphaRef. It is responsible for storing the state of the AlphaRef.
 * <p/>
 * The AlpaRefTranlocal also implements the Transaction interface because it can be
 * used as a lockOwner. This is done as a performance optimization.
 *
 * @param <E>
 */
class AlphaRefTranlocal<E> extends AlphaTranlocal implements Transaction {

    //field belonging to the stm.
    AlphaRef ___txObject;
    AlphaRefTranlocal ___origin;

    E value;

    AlphaRefTranlocal(AlphaRefTranlocal<E> origin) {
        this.___origin = origin;
        this.___txObject = origin.___txObject;
        this.value = origin.value;
    }

    AlphaRefTranlocal(AlphaRef<E> owner) {
        this(owner, null);
    }

    AlphaRefTranlocal(AlphaRef<E> owner, E value) {
        this.___txObject = owner;
        this.value = value;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new AlphaRefTranlocal(this);
    }

    @Override
    public AlphaTranlocal getOrigin() {
        return ___origin;
    }

    @Override
    public AlphaTransactionalObject getTransactionalObject() {
        return ___txObject;
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        this.___writeVersion = writeVersion;
        this.___origin = null;
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

        if (___origin.value != this.value) {
            return true;
        }

        return false;
    }

    @Override
    public void abort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransactionConfig getConfig() {
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

