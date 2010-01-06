package org.multiverse.datastructures.refs.manual;

import org.multiverse.api.GlobalStmInstance;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.StmUtils.retry;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.datastructures.refs.ManagedRef;
import org.multiverse.stms.alpha.*;
import org.multiverse.stms.alpha.mixins.FastAtomicObjectMixin;
import org.multiverse.templates.AtomicTemplate;

import static java.lang.String.format;

/**
 * A manual instrumented {@link org.multiverse.datastructures.refs.ManagedRef} implementation. If this class is used,
 * you don't need to worry about instrumentation/javaagents and stuff like this.
 * <p/>
 * It is added to get the Akka project up and running, but probably will removed when the instrumentation is 100% up and
 * running and this can be done compiletime instead of messing with javaagents.
 *
 * @author Peter Veentjer
 */
public final class Ref<E> extends FastAtomicObjectMixin implements ManagedRef<E> {

    /**
     * Creates a committed ref with a null value using the Stm in the {@link GlobalStmInstance}.
     *
     * @return the created ref.
     *
     * @see #createCommittedRef(Stm, Object)
     */
    public static <E> Ref<E> createCommittedRef() {
        return createCommittedRef(getGlobalStmInstance(), null);
    }

    /**
     * Creates a committed ref with a null value.
     *
     * @param stm the {@link Stm} used for committing the ref.
     * @return the created ref.
     *
     * @see #createCommittedRef(Stm, Object)
     */
    public static <E> Ref<E> createCommittedRef(Stm stm) {
        return createCommittedRef(stm, null);
    }

    /**
     * Creates a committed ref with the given value using the Stm in the {@link GlobalStmInstance}.
     *
     * @param value the initial value of the Ref.
     * @return the created ref.
     *
     * @see #createCommittedRef(Stm, Object)
     */
    public static <E> Ref<E> createCommittedRef(E value) {
        return createCommittedRef(getGlobalStmInstance(), value);
    }

    /**
     * Creates a committed ref with the given value and using the given Stm.
     * <p/>
     * This factory method should be called when one doesn't want to lift on the current transaction, but you want
     * something to be committed whatever happens. In the future behavior will be added propagation levels. But for the
     * time being this is the 'expect_new' implementation of this propagation level.
     * <p/>
     * If the value is an atomicobject or has a reference to it (perhaps indirectly), and the transaction this
     * atomicobject is created in is aborted (or hasn't committed) yet, you will get the dreaded {@link
     * org.multiverse.api.exceptions.LoadUncommittedException}.
     *
     * @param stm   the {@link Stm} used for committing the ref.
     * @param value the initial value of the ref. The value is allowed to be null.
     * @return the created ref.
     */
    public static <E> Ref<E> createCommittedRef(Stm stm, E value) {
        Transaction t = stm.startUpdateTransaction("createRef");
        Ref<E> ref = new Ref<E>(t, value);
        t.commit();
        return ref;
    }

    public Ref() {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal<E>) ((AlphaTransaction) t).load(Ref.this);
                return null;
            }
        }.execute();
    }

    public Ref(Transaction t) {
        RefTranlocal<E> tranlocal = (RefTranlocal<E>) ((AlphaTransaction) t).load(Ref.this);
    }

    public Ref(final E value) {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal<E>) ((AlphaTransaction) t).load(Ref.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public Ref(Transaction t, E value) {
        RefTranlocal<E> tranlocal = (RefTranlocal<E>) ((AlphaTransaction) t).load(Ref.this);
        tranlocal.value = value;
    }

    public E get() {
        return new AtomicTemplate<E>(true) {
            @Override
            public E execute(Transaction t) {
                return get(t);
            }
        }.execute();
    }

    public E get(Transaction t) {
        RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
        return tranlocal.value;
    }

    @Override
    public E getOrAwait() {
        return new AtomicTemplate<E>(true) {
            @Override
            public E execute(Transaction t) throws Exception {
                return getOrAwait(t);
            }
        }.execute();
    }

    public E getOrAwait(Transaction t) {
        RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
        if (tranlocal.value == null) {
            retry();
        }

        return tranlocal.value;
    }


    @Override
    public E set(final E newRef) {
        return new AtomicTemplate<E>() {
            @Override
            public E execute(Transaction t) throws Exception {
                return set(t, newRef);
            }
        }.execute();
    }

    public E set(Transaction t, E newValue) {
        RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);

        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }

        E oldValue = tranlocal.value;
        tranlocal.value = newValue;
        return oldValue;
    }

    @Override
    public boolean isNull() {
        return new AtomicTemplate<Boolean>(true) {
            @Override
            public Boolean execute(Transaction t) throws Exception {
                return isNull(t);
            }
        }.execute();
    }

    public boolean isNull(Transaction t) {
        RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
        return tranlocal.value == null;
    }

    @Override
    public E clear() {
        return new AtomicTemplate<E>() {
            @Override
            public E execute(Transaction t) throws Exception {
                return clear(t);
            }
        }.execute();
    }

    public E clear(Transaction t) {
        RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }

        E oldValue = tranlocal.value;
        tranlocal.value = null;
        return oldValue;
    }

    @Override
    public String toString() {
        return new AtomicTemplate<String>(true) {
            @Override
            public String execute(Transaction t) throws Exception {
                return Ref.this.toString(t);
            }

        }.execute();
    }

    public String toString(Transaction t) {
        RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
        if (tranlocal.value == null) {
            return "Ref(reference=null)";
        } else {
            return format("Ref(reference=%s)", tranlocal.value);
        }
    }

    @Override
    public RefTranlocal<E> ___loadUpdatable(long readVersion) {
        RefTranlocal<E> origin = (RefTranlocal<E>) ___load(readVersion);
        if (origin == null) {
            return new RefTranlocal<E>(this);
        } else {
            return new RefTranlocal<E>(origin);
        }
    }
}

class RefTranlocal<E> extends AlphaTranlocal {

    //field belonging to the stm.
    Ref ___atomicObject;
    RefTranlocal ___origin;

    E value;

    RefTranlocal(RefTranlocal<E> origin) {
        this.___origin = origin;
        this.___atomicObject = origin.___atomicObject;
        this.value = origin.value;
    }

    RefTranlocal(Ref<E> owner) {
        this(owner, null);
    }

    RefTranlocal(Ref<E> owner, E value) {
        this.___atomicObject = owner;
        this.value = value;
    }

    @Override
    public AlphaAtomicObject getAtomicObject() {
        return ___atomicObject;
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        this.___writeVersion = writeVersion;
        this.___origin = null;
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new RefTranlocalSnapshot<E>(this);
    }

    @Override
    public DirtinessStatus getDirtinessStatus() {
        if (___writeVersion > 0) {
            return DirtinessStatus.readonly;
        } else if (___origin == null) {
            return DirtinessStatus.fresh;
        } else if (___origin.value != this.value) {
            return DirtinessStatus.dirty;
        } else {
            return DirtinessStatus.clean;
        }
    }
}

class RefTranlocalSnapshot<E> extends AlphaTranlocalSnapshot {

    final RefTranlocal ___tranlocal;
    final E value;

    RefTranlocalSnapshot(RefTranlocal<E> tranlocal) {
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