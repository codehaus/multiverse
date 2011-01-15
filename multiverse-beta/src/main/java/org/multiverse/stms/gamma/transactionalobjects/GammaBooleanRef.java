package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.BooleanFunction;
import org.multiverse.api.predicates.BooleanPredicate;
import org.multiverse.api.references.BooleanRef;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

public final class GammaBooleanRef extends AbstractGammaRef implements BooleanRef {

    public GammaBooleanRef(GammaStm stm) {
        this(stm, false);
    }

    public GammaBooleanRef(GammaStm stm, boolean b) {
        super(stm, TYPE_BOOLEAN);
        this.long_value = asLong(b);
        this.version = VERSION_UNCOMMITTED + 1;
    }

    @Override
    public final boolean set(final boolean value) {
        return set(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final boolean set(final Transaction tx, final boolean value) {
        return set(asGammaTransaction(tx), value);
    }

    public final boolean set(final GammaTransaction tx, final boolean value) {
        openForWrite(tx, LOCKMODE_NONE).long_value = asLong(value);
        return value;
    }

    @Override
    public final boolean get() {
        return get(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public final boolean get(final Transaction tx) {
        return get(asGammaTransaction(tx));
    }

    public final boolean get(final GammaTransaction tx) {
        return asBoolean(openForRead(tx, LOCKMODE_NONE).long_value);
    }

    @Override
    public final boolean atomicGet() {
        return asBoolean(atomicLongGet());
    }

    @Override
    public final boolean atomicWeakGet() {
        return asBoolean(long_value);
    }

    @Override
    public final boolean atomicSet(final boolean newValue) {
        atomicGetAndSet(newValue);
        return newValue;
    }

    @Override
    public final boolean atomicGetAndSet(final boolean newValue) {
        //todo:
        throw new TodoException();
    }

    @Override
    public final boolean getAndSet(final boolean value) {
        return getAndSet(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final boolean getAndSet(final Transaction tx, final boolean value) {
        return getAndSet(asGammaTransaction(tx), value);
    }

    public final boolean getAndSet(final GammaTransaction tx, final boolean value) {
        GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        boolean oldValue = asBoolean(tranlocal.long_value);
        tranlocal.long_value = asLong(value);
        return oldValue;
    }

    @Override
    public final void commute(final BooleanFunction function) {
        commute(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final void commute(final Transaction tx, final BooleanFunction function) {
        commute(asGammaTransaction(tx), function);
    }

    public final void commute(final GammaTransaction tx, final BooleanFunction function) {
        //todo:
        throw new TodoException();
    }

    @Override
    public final boolean getAndAlter(final BooleanFunction function) {
        return getAndAlter(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final boolean getAndAlter(final Transaction tx, final BooleanFunction function) {
        return getAndAlter(asGammaTransaction(tx), function);
    }

    public final boolean getAndAlter(final GammaTransaction tx, final BooleanFunction function) {
        return alter(tx, function, true);
    }

    @Override
    public final boolean alterAndGet(final BooleanFunction function) {
        return alterAndGet(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final boolean alterAndGet(final Transaction tx, final BooleanFunction function) {
        return alterAndGet(asGammaTransaction(tx), function);
    }

    public final boolean alterAndGet(final GammaTransaction tx, final BooleanFunction function) {
        return alter(tx, function, false);
    }

    public final boolean alter(final GammaTransaction tx, final BooleanFunction function, final boolean returnOld) {
        throw new TodoException();
    }

    @Override
    public final boolean atomicAlterAndGet(final BooleanFunction function) {
        return atomicAlter(function, false);
    }

    @Override
    public final boolean atomicGetAndAlter(final BooleanFunction function) {
        return atomicAlter(function, true);
    }

    public final boolean atomicAlter(final BooleanFunction function, boolean returnOld) {
        //todo
        throw new TodoException();
    }

    @Override
    public final boolean atomicCompareAndSet(final boolean expectedValue, final boolean newValue) {
        //todo:
        throw new TodoException();
    }

    @Override
    public final void await(final boolean value) {
        await(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final void await(final Transaction tx, final boolean value) {
        await(asGammaTransaction(tx), value);
    }

    public final void await(final GammaTransaction tx, final boolean value) {
        if (asBoolean(openForRead(tx, LOCKMODE_NONE).long_value) != value) {
            tx.retry();
        }
    }

    @Override
    public final void await(final BooleanPredicate predicate) {
        await(getRequiredThreadLocalGammaTransaction(), predicate);
    }

    @Override
    public final void await(final Transaction tx, final BooleanPredicate predicate) {
        await(asGammaTransaction(tx), predicate);
    }

    public final void await(final GammaTransaction tx, final BooleanPredicate predicate) {
        //todo
        throw new TodoException();
    }

    @Override
    public String toDebugString() {
        return String.format("GammaBooleanRef{orec=%s, version=%s, value=%s, hasListeners=%s)",
                ___toOrecString(), version, asBoolean(long_value), listeners != null);
    }

    public static boolean asBoolean(long value) {
        return value == 1;
    }

    public static long asLong(boolean b) {
        return b ? 1 : 0;
    }
}
