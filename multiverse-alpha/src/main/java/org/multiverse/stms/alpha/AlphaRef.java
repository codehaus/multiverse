package org.multiverse.stms.alpha;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.transactional.TransactionalReference;
import org.multiverse.utils.TodoException;

import static java.lang.String.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;

/**
 * A manual instrumented {@link org.multiverse.transactional.TransactionalReference} implementation. If this class
 * is used,you don't need to worry about instrumentation/javaagents and stuff like this.
 * <p/>
 * It is added to get the Akka project up and running, but probably will removed when the instrumentation is 100% up
 * and running and this can be done compiletime instead of messing with javaagents.
 * <p/>
 * <h3>Lifting on a transaction</h3>
 * All methods automatically lift on a transaction if one is available, but to reduce the need for extra object
 * creation and unwanted ThreadLocal access, there also are methods available that have a
 * {@link org.multiverse.api.Transaction} as first argument.
 * <h3>TransactionFactory</h3>
 * All methods of this AlphaRef also have a version that accepts a {@link TransactionFactory}. TransactionFactories
 * can be quite expensive to create, so it is best to create them up front and reuse them. TransactionFactories
 * are threadsafe to use, so no worries about that as well.
 * <p/>
 * <h3>Performance</h3>
 * If performance of the AlphaRef becomes an issue, there is some room for improvement. Make sure that the transaction
 * familynames are set, and if needed we can also remove the TransactionTemplate because it causes additional
 * unwanted objects to be created.
 * <h3>Relying on GlobalStmInstance</h3>
 * This Ref implementation can be used without depending on the GlobalStmInstance (so you could create a local
 * one stm instance). If this is done, only the methods that rely on a Transaction or TransactionFactory
 * should be used.
 *
 * @author Peter Veentjer
 */
public final class AlphaRef<E> extends DefaultTxObjectMixin implements TransactionalReference<E> {
    private static final String CREATE_COMMITTED_FAMILY_NAME = AlphaRef.class.getName() + ".createCommitted(Stm,E)";

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
     * transactionalobject is created in is aborted (or hasn't committed) yet, you will get the dreaded {@link
     * org.multiverse.api.exceptions.LoadUncommittedException}.
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

    private final static TransactionFactory initTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setFamilyName(AlphaRef.class.getName() + ".init()")
            .setSmartTxLengthSelector(true)
            .setReadonly(false)
            .setAutomaticReadTracking(false)
            .setSmartTxLengthSelector(true).build();


    /**
     * Creates a new Ref.
     * <p/>
     * This method relies on the ThreadLocalTransaction and GlobalStmInstance.
     */
    public AlphaRef() {
        this(initTxFactory, null);
    }

    /**
     * Creates a new Ref using the provided transaction.
     * <p/>
     * This method does not rely on a ThreadLocalTransaction and GlobalStmInstance.
     *
     * @param tx the Transaction used
     */
    public AlphaRef(Transaction tx) {
        ((AlphaTransaction) tx).openForWrite(AlphaRef.this);
    }

    /**
     * Creates a new Ref with the provided value.
     * <p/>
     * This method relies on the ThreadLocalTransaction.
     * If no transaction is found, it also relies on the GlobalStmInstance.
     *
     * @param value the value this Ref should have.
     */
    public AlphaRef(final E value) {
        this(initTxFactory, value);
    }

    public AlphaRef(TransactionFactory txFactory, final E value) {
        new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal<E>) ((AlphaTransaction) tx).openForWrite(AlphaRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public AlphaRef(Transaction tx, E value) {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal<E>) ((AlphaTransaction) tx).openForWrite(AlphaRef.this);
        tranlocal.value = value;
    }

    private final static TransactionFactory getTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setFamilyName(AlphaRef.class.getName() + ".get()")
            .setSmartTxLengthSelector(true)
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    public E get() {
        return get(getTxFactory);
    }

    public E get(TransactionFactory txFactory) {
        return new TransactionTemplate<E>(txFactory) {
            @Override
            public E execute(Transaction tx) {
                return get(tx);
            }
        }.execute();
    }

    public E get(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal) ((AlphaTransaction) tx).openForRead(AlphaRef.this);
        return tranlocal.value;
    }

    private final static TransactionFactory getOrAwaitTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(true)
            .setFamilyName(AlphaRef.class.getName() + ".getOrAwait()")
            .setSmartTxLengthSelector(true)
            .setAutomaticReadTracking(true).build();

    @Override
    public E getOrAwait() {
        return getOrAwait(getOrAwaitTxFactory);
    }

    public E getOrAwait(TransactionFactory txFactory) {
        return new TransactionTemplate<E>(txFactory) {
            @Override
            public E execute(Transaction t) throws Exception {
                return getOrAwait(t);
            }
        }.execute();
    }

    public E getOrAwait(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal) ((AlphaTransaction) tx).openForRead(AlphaRef.this);
        if (tranlocal.value == null) {
            retry();
        }

        return tranlocal.value;
    }

    @Override
    public E getOrAwaitInterruptibly() throws InterruptedException {
        throw new TodoException("Not implemented yet");
    }

    private final static TransactionFactory setTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(false)
            .setFamilyName(AlphaRef.class.getName() + ".set()")
            .setSmartTxLengthSelector(true)
            .setAutomaticReadTracking(false).build();

    @Override
    public E set(final E newRef) {
        return set(setTxFactory, newRef);
    }

    public E set(TransactionFactory txFactory, final E newRef) {
        return new TransactionTemplate<E>(txFactory) {
            @Override
            public E execute(Transaction tx) throws Exception {
                return set(tx, newRef);
            }
        }.execute();
    }

    public E set(Transaction tx, E newValue) {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal) ((AlphaTransaction) tx).openForWrite(AlphaRef.this);
        E oldValue = tranlocal.value;
        tranlocal.value = newValue;
        return oldValue;
    }

    private final static TransactionFactory isNullTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setFamilyName(AlphaRef.class.getName() + ".isNull()")
            .setSmartTxLengthSelector(true)
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    @Override
    public boolean isNull() {
        return isNull(isNullTxFactory);
    }

    public boolean isNull(TransactionFactory txFactory) {
        return new TransactionTemplate<Boolean>(txFactory) {
            @Override
            public Boolean execute(Transaction t) throws Exception {
                return isNull(t);
            }
        }.execute();
    }

    public boolean isNull(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal) ((AlphaTransaction) tx).openForRead(AlphaRef.this);
        return tranlocal.value == null;
    }

    private final static TransactionFactory clearTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setFamilyName(AlphaRef.class.getName() + ".clear()")
            .setSmartTxLengthSelector(true)
            .setReadonly(false)
            .setAutomaticReadTracking(false).build();


    @Override
    public E clear() {
        return clear(clearTxFactory);
    }

    public E clear(TransactionFactory txFactory) {
        return new TransactionTemplate<E>(txFactory) {
            @Override
            public E execute(Transaction tx) throws Exception {
                return clear(tx);
            }
        }.execute();
    }

    public E clear(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal) ((AlphaTransaction) tx).openForWrite(AlphaRef.this);
        E oldValue = tranlocal.value;
        tranlocal.value = null;
        return oldValue;
    }

    private final static TransactionFactory toStringTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setFamilyName(AlphaRef.class.getName() + ".toString()")
            .setSmartTxLengthSelector(true)
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    @Override
    public String toString() {
        return toString(toStringTxFactory);
    }

    public String toString(TransactionFactory txFactory) {
        return new TransactionTemplate<String>(txFactory) {
            @Override
            public String execute(Transaction tx) throws Exception {
                return AlphaRef.this.toString(tx);
            }

        }.execute();
    }

    public String toString(Transaction tx) {
        AlphaRefTranlocal<E> tranlocal = (AlphaRefTranlocal) ((AlphaTransaction) tx).openForRead(AlphaRef.this);
        if (tranlocal.value == null) {
            return "DefaultTransactionalReference(reference=null)";
        } else {
            return format("DefaultTransactionalReference(reference=%s)", tranlocal.value);
        }
    }

    @Override
    public AlphaRefTranlocal<E> ___openUnconstructed() {
        return new AlphaRefTranlocal<E>(this);
    }
}


class AlphaRefTranlocal<E> extends AlphaTranlocal {

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