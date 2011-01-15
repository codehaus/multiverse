package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.api.predicates.IntPredicate;
import org.multiverse.api.references.IntRef;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

public final class GammaIntRef extends AbstractGammaRef implements IntRef {

    public GammaIntRef(GammaStm stm) {
        this(stm, 0);
    }

    public GammaIntRef(GammaStm stm, int value) {
        super(stm, 0);//todo: type
    }

    @Override
    public final int set(final int value) {
        return set(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final int set(final Transaction tx, final int value) {
        return set(asGammaTransaction(tx), value);
    }

    public final int set(final GammaTransaction tx, final int value) {
        GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value = value;
        return value;
    }

    @Override
    public final int get() {
        return get(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public final int get(final Transaction tx) {
        return get(asGammaTransaction(tx));
    }

    public final int get(final GammaTransaction tx) {
        GammaRefTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);
        return (int) tranlocal.long_value;
    }

    @Override
    public final int atomicGet() {
        throw new TodoException();
    }

    @Override
    public final int atomicWeakGet() {
        throw new TodoException();
    }

    @Override
    public final int atomicSet(final int newValue) {
        throw new TodoException();
    }

    @Override
    public final int atomicGetAndSet(final int newValue) {
        throw new TodoException();
    }

    @Override
    public final int getAndSet(final int value) {
        return getAndSet(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final int getAndSet(final Transaction tx, final int value) {
        return getAndSet(asGammaTransaction(tx), value);
    }

    public final int getAndSet(final GammaTransaction tx, final int value) {
        GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        int oldValue = (int) tranlocal.long_value;
        tranlocal.long_value = value;
        return oldValue;
    }

    @Override
    public final void commute(final IntFunction function) {
        commute(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final void commute(final Transaction tx, final IntFunction function) {
        commute(asGammaTransaction(tx), function);
    }

    public final void commute(final GammaTransaction tx, final IntFunction function) {
        throw new TodoException();
    }

    @Override
    public final int atomicAlterAndGet(final IntFunction function) {
        throw new TodoException();
    }

    @Override
    public final int alterAndGet(final IntFunction function) {
        return alterAndGet(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final int alterAndGet(final Transaction tx, final IntFunction function) {
        return alterAndGet(asGammaTransaction(tx), function);
    }

    public final int alterAndGet(final GammaTransaction tx, final IntFunction function) {
        throw new TodoException();
    }

    @Override
    public final int atomicGetAndAlter(final IntFunction function) {
        throw new TodoException();
    }

    @Override
    public final int getAndAlter(final IntFunction function) {
        return getAndAlter(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final int getAndAlter(final Transaction tx, final IntFunction function) {
        return getAndAlter(asGammaTransaction(tx), function);
    }

    public final int getAndAlter(final GammaTransaction tx, final IntFunction function) {
        throw new TodoException();
    }

    @Override
    public final boolean atomicCompareAndSet(final int expectedValue, final int newValue) {
        throw new TodoException();
    }

    @Override
    public final int atomicGetAndIncrement(final int amount) {
        throw new TodoException();
    }

    @Override
    public final int getAndIncrement(final int amount) {
        return getAndIncrement(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public final int getAndIncrement(final Transaction tx, final int amount) {
        return getAndIncrement(asGammaTransaction(tx), amount);
    }

    public final int getAndIncrement(final GammaTransaction tx, final int amount) {
        throw new TodoException();
    }

    @Override
    public final int atomicIncrementAndGet(final int amount) {
        throw new TodoException();
    }

    @Override
    public final int incrementAndGet(final int amount) {
        return incrementAndGet(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public final int incrementAndGet(final Transaction tx, final int amount) {
        return incrementAndGet(asGammaTransaction(tx), amount);
    }

    public final int incrementAndGet(final GammaTransaction tx, final int amount) {
        throw new TodoException();
    }

    @Override
    public final void increment() {
        increment(getRequiredThreadLocalGammaTransaction(), 1);
    }

    @Override
    public final void increment(final Transaction tx) {
        increment(asGammaTransaction(tx), 1);
    }

    @Override
    public final void increment(final int amount) {
        increment(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public final void increment(final Transaction tx, final int amount) {
        increment(asGammaTransaction(tx), amount);
    }

    public final void increment(final GammaTransaction tx, final int amount) {
        throw new TodoException();
    }

    @Override
    public final void decrement() {
        increment(getRequiredThreadLocalGammaTransaction(), -1);
    }

    @Override
    public final void decrement(Transaction tx) {
        increment(asGammaTransaction(tx), -1);
    }

    @Override
    public final void decrement(final int amount) {
        increment(getRequiredThreadLocalGammaTransaction(), -amount);
    }

    @Override
    public final void decrement(final Transaction tx, final int amount) {
        increment(asGammaTransaction(tx), -amount);
    }

    @Override
    public final void await(final int value) {
        await(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final void await(final Transaction tx, final int value) {
        await(asGammaTransaction(tx), value);
    }

    public final void await(final GammaTransaction tx, final int value) {
        throw new TodoException();
    }

    @Override
    public final void await(final IntPredicate predicate) {
        await(getRequiredThreadLocalGammaTransaction(), predicate);
    }

    @Override
    public final void await(final Transaction tx, final IntPredicate predicate) {
        await(asGammaTransaction(tx), predicate);
    }

    public final void await(final GammaTransaction tx, final IntPredicate predicate) {
        throw new TodoException();
    }

    @Override
    public final String toDebugString() {
        throw new TodoException();
    }
}
