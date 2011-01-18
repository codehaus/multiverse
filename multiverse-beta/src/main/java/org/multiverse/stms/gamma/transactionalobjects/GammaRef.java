package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.Function;
import org.multiverse.api.predicates.Predicate;
import org.multiverse.api.references.Ref;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaStmUtils.asGammaTransaction;
import static org.multiverse.stms.gamma.GammaStmUtils.getRequiredThreadLocalGammaTransaction;

/**
 * A {@link Ref} tailored for the {@link GammaStm}.
 *
 * @param <E>
 * @author Peter Veentjer.
 */
public final class GammaRef<E> extends AbstractGammaRef implements Ref<E> {

    public GammaRef(final GammaTransaction tx) {
        this(tx, null);
    }

    public GammaRef(final GammaTransaction tx, final E value) {
        super(tx.getConfiguration().stm, TYPE_REF);

        tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);
        GammaRefTranlocal tranlocal = openForConstruction(tx);
        tranlocal.ref_value = value;
    }

    public GammaRef(final GammaStm stm) {
        this(stm, null);
    }

    public GammaRef(final GammaStm stm, final E value) {
        super(stm, TYPE_REF);

        this.ref_value = value;
        //noinspection PointlessArithmeticExpression
        this.version = VERSION_UNCOMMITTED + 1;
    }

    @Override
    public final E set(final E value) {
        return set(getRequiredThreadLocalTransaction(), value);
    }

    @Override
    public final E set(final Transaction tx, final E value) {
        return set(asGammaTransaction(tx), value);
    }

    public final E set(final GammaTransaction tx, final E value) {
        final GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        tranlocal.ref_value = value;
        return value;
    }

    @Override
    public final E get() {
        return get(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public final E get(final Transaction tx) {
        return get(asGammaTransaction(tx));
    }

    public final E get(final GammaTransaction tx) {
        return (E) openForRead(tx, LOCKMODE_NONE).ref_value;
    }

    @Override
    public final E atomicGet() {
        return (E) atomicObjectGet();
    }

    @Override
    public final E atomicWeakGet() {
        return (E) ref_value;
    }

    @Override
    public final E atomicSet(final E newValue) {
        atomicGetAndSet(newValue);
        return newValue;
    }

    @Override
    public final E atomicGetAndSet(final E newValue) {
        //todo:
        throw new TodoException();
    }

    @Override
    public final E getAndSet(final E value) {
        return getAndSet(getRequiredThreadLocalTransaction(), value);
    }

    @Override
    public final E getAndSet(final Transaction tx, final E value) {
        return getAndSet(asGammaTransaction(tx), value);
    }

    public final E getAndSet(final GammaTransaction tx, final E value) {
        GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        E oldValue = (E) tranlocal.ref_value;
        tranlocal.ref_value = value;
        return oldValue;
    }

    @Override
    public final void commute(final Function<E> function) {
        commute(getRequiredThreadLocalTransaction(), function);
    }

    @Override
    public final void commute(final Transaction tx, final Function<E> function) {
        commute(asGammaTransaction(tx), function);
    }

    public final void commute(final GammaTransaction tx, final Function<E> function) {
        openForCommute(tx, function);
    }

    @Override
    public final E atomicAlterAndGet(final Function<E> function) {
        return atomicAlter(function, false);
    }

    @Override
    public final E atomicGetAndAlter(final Function<E> function) {
        return atomicAlter(function, true);
    }

    private E atomicAlter(final Function<E> function, final boolean returnOld) {
        //todo
        throw new TodoException();
    }

    @Override
    public final E alterAndGet(final Function<E> function) {
        return alterAndGet(getRequiredThreadLocalTransaction(), function);
    }

    @Override
    public final E alterAndGet(final Transaction tx, final Function<E> function) {
        return alterAndGet(asGammaTransaction(tx), function);
    }

    public final E alterAndGet(final GammaTransaction tx, final Function<E> function) {
        return alter(tx, function, false);
    }

    @Override
    public final E getAndAlter(final Function<E> function) {
        return getAndAlter(getRequiredThreadLocalTransaction(), function);
    }

    @Override
    public final E getAndAlter(final Transaction tx, final Function<E> function) {
        return getAndAlter(asGammaTransaction(tx), function);
    }

    public final E getAndAlter(final GammaTransaction tx, final Function<E> function) {
        return alter(tx, function, true);
    }

    private E alter(final GammaTransaction tx, final Function<E> function, final boolean returnOld) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (function == null) {
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        final GammaRefTranlocal write = openForWrite(tx, LOCKMODE_NONE);

        boolean abort = true;

        try {
            E oldValue = (E) write.ref_value;
            write.ref_value = function.call(oldValue);
            abort = false;
            return returnOld ? oldValue : (E) write.ref_value;
        } finally {
            if (abort) {
                tx.abort();
            }
        }
    }

    @Override
    public final boolean atomicCompareAndSet(final E expectedValue, final E newValue) {
        //todo:
        throw new TodoException();
    }

    @Override
    public final boolean isNull() {
        return isNull(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public final boolean isNull(final Transaction tx) {
        return isNull(asGammaTransaction(tx));
    }

    public final boolean isNull(final GammaTransaction tx) {
        return openForRead(tx, LOCKMODE_NONE).ref_value == null;
    }

    @Override
    public final boolean atomicIsNull() {
        return atomicGet() == null;
    }

    @Override
    public final E awaitNotNullAndGet() {
        return awaitNotNullAndGet(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public final E awaitNotNullAndGet(final Transaction tx) {
        return awaitNotNullAndGet(asGammaTransaction(tx));
    }

    public final E awaitNotNullAndGet(final GammaTransaction tx) {
        final GammaRefTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);

        if (tranlocal.ref_value == null) {
            tx.retry();
        }

        return (E) tranlocal.ref_value;
    }

    @Override
    public final void awaitNull() {
        await(getRequiredThreadLocalGammaTransaction(), (E) null);
    }

    @Override
    public final void awaitNull(final Transaction tx) {
        await(asGammaTransaction(tx), (E) null);
    }

    public final void awaitNull(final GammaTransaction tx) {
        await(tx, (E) null);
    }

    @Override
    public final void await(final E value) {
        await(getRequiredThreadLocalTransaction(), value);
    }

    @Override
    public final void await(final Transaction tx, final E value) {
        await(asGammaTransaction(tx), value);
    }

    public final void await(final GammaTransaction tx, final E value) {
        //noinspection ObjectEquality
        if (openForRead(tx, LOCKMODE_NONE).ref_value != value) {
            tx.retry();
        }
    }

    @Override
    public final void await(final Predicate<E> predicate) {
        await(getRequiredThreadLocalTransaction(), predicate);
    }

    @Override
    public final void await(final Transaction tx, final Predicate<E> predicate) {
        await(asGammaTransaction(tx), predicate);
    }

    public final void await(final GammaTransaction tx, final Predicate<E> predicate) {
        final GammaRefTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);
        boolean abort = true;
        try {
            if (!predicate.evaluate((E) tranlocal.ref_value)) {
                tx.retry();
            }
            abort = false;
        } finally {
            if (abort) {
                tx.abort();
            }
        }
    }

    @Override
    public String toDebugString() {
        return String.format("GammaRef{orec=%s, version=%s, value=%s, hasListeners=%s)",
                ___toOrecString(), version, ref_value, listeners != null);
    }
}
