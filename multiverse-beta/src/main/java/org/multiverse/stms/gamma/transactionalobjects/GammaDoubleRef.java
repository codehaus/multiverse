package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.DoubleFunction;
import org.multiverse.api.predicates.DoublePredicate;
import org.multiverse.api.references.DoubleRef;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.multiverse.stms.gamma.GammaStmUtils.*;

public final class GammaDoubleRef extends AbstractGammaRef implements DoubleRef {

    public GammaDoubleRef(final GammaTransaction tx) {
        this(tx, 0);
    }

    public GammaDoubleRef(final GammaTransaction tx, final double value) {
        super(tx.getConfiguration().stm, TYPE_DOUBLE);

        tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);
        GammaRefTranlocal tranlocal = openForConstruction(tx);
        tranlocal.long_value = doubleAsLong(value);
    }

    public GammaDoubleRef(GammaStm stm) {
        this(stm, 0);
    }

    public GammaDoubleRef(GammaStm stm, double value) {
        super(stm, TYPE_DOUBLE);
        this.long_value = doubleAsLong(value);
        //noinspection PointlessArithmeticExpression
        this.version = VERSION_UNCOMMITTED + 1;
    }

    @Override
    public final double set(final double value) {
        return set(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final double set(final Transaction tx, final double value) {
        return set(asGammaTransaction(tx), value);
    }

    public final double set(final GammaTransaction tx, final double value) {
        openForWrite(tx, LOCKMODE_NONE).long_value = doubleAsLong(value);
        return value;
    }

    @Override
    public final double get() {
        return get(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public final double get(final Transaction tx) {
        return get(asGammaTransaction(tx));
    }

    public final double get(final GammaTransaction tx) {
        return longAsDouble(openForRead(tx, LOCKMODE_NONE).long_value);
    }

    @Override
    public final double atomicGet() {
        return longAsDouble(atomicGetLong());
    }

    @Override
    public final double atomicWeakGet() {
        return longAsDouble(long_value);
    }

    @Override
    public final double atomicSet(final double newValue) {
        return longAsDouble(atomicSetLong(doubleAsLong(newValue), false));
    }

    @Override
    public final double atomicGetAndSet(final double newValue) {
        return longAsDouble(atomicSetLong(doubleAsLong(newValue), true));
    }

    @Override
    public final double getAndSet(final double value) {
        return getAndSet(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final double getAndSet(final Transaction tx, final double value) {
        return getAndSet(asGammaTransaction(tx), value);
    }

    public final double getAndSet(final GammaTransaction tx, final double value) {
        GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        double oldValue = longAsDouble(tranlocal.long_value);
        tranlocal.long_value = doubleAsLong(value);
        return oldValue;
    }

    @Override
    public final void commute(final DoubleFunction function) {
        commute(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final void commute(final Transaction tx, final DoubleFunction function) {
        commute(asGammaTransaction(tx), function);
    }

    public final void commute(final GammaTransaction tx, final DoubleFunction function) {
        openForCommute(tx, function);
    }

    @Override
    public final double atomicAlterAndGet(final DoubleFunction function) {
        return atomicAlterSupport(function, false);
    }

    @Override
    public final double atomicGetAndAlter(final DoubleFunction function) {
        return atomicAlterSupport(function, true);
    }

    public final double atomicAlterSupport(final DoubleFunction function, boolean returnOld) {
        //todo:
        throw new TodoException();
    }

    @Override
    public final double alterAndGet(final DoubleFunction function) {
        return alterAndGet(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final double alterAndGet(final Transaction tx, final DoubleFunction function) {
        return alterAndGet(asGammaTransaction(tx), function);
    }

    public final double alterAndGet(final GammaTransaction tx, final DoubleFunction function) {
        return alter(tx, function, false);
    }

    @Override
    public final double getAndAlter(final DoubleFunction function) {
        return getAndAlter(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final double getAndAlter(final Transaction tx, final DoubleFunction function) {
        return getAndAlter(asGammaTransaction(tx), function);
    }

    public final double getAndAlter(final GammaTransaction tx, final DoubleFunction function) {
        return alter(tx, function, true);
    }

    public final double alter(final GammaTransaction tx, final DoubleFunction function, boolean returnOld) {
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
            double oldValue = longAsDouble(write.long_value);
            write.long_value = doubleAsLong(function.call(oldValue));
            abort = false;
            return returnOld ? oldValue : longAsDouble(write.long_value);
        } finally {
            if (abort) {
                tx.abort();
            }
        }
    }


    @Override
    public final boolean atomicCompareAndSet(final double expectedValue, final double newValue) {
        return atomicCompareAndSetLong(doubleAsLong(expectedValue), doubleAsLong(newValue));
    }

    @Override
    public final double getAndIncrement(final double amount) {
        return getAndIncrement(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public final double getAndIncrement(final Transaction tx, final double amount) {
        return getAndIncrement(asGammaTransaction(tx), amount);
    }

    public final double getAndIncrement(final GammaTransaction tx, final double amount) {
        GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        double oldValue = longAsDouble(tranlocal.long_value);
        tranlocal.long_value = doubleAsLong(oldValue + amount);
        return oldValue;
    }

    @Override
    public final double atomicGetAndIncrement(final double amount) {
        return atomicIncrement(amount, true);
    }

    @Override
    public final double atomicIncrementAndGet(final double amount) {
        return atomicIncrement(amount, true);
    }

    private double atomicIncrement(final double amount, boolean returnOld) {
        throw new TodoException();
    }

    @Override
    public final double incrementAndGet(final double amount) {
        return incrementAndGet(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public final double incrementAndGet(final Transaction tx, final double amount) {
        return incrementAndGet(asGammaTransaction(tx), amount);
    }

    public final double incrementAndGet(final GammaTransaction tx, final double amount) {
        GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        double result = longAsDouble(tranlocal.long_value) + amount;
        tranlocal.long_value = doubleAsLong(result);
        return result;
    }

    @Override
    public final void await(final double value) {
        await(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final void await(final Transaction tx, final double value) {
        await(asGammaTransaction(tx), value);
    }

    public final void await(final GammaTransaction tx, final double value) {
        if (longAsDouble(openForRead(tx, LOCKMODE_NONE).long_value) != value) {
            tx.retry();
        }
    }

    @Override
    public final void await(final DoublePredicate predicate) {
        await(getRequiredThreadLocalGammaTransaction(), predicate);
    }

    @Override
    public final void await(final Transaction tx, final DoublePredicate predicate) {
        await(asGammaTransaction(tx), predicate);
    }

    public final void await(final GammaTransaction tx, final DoublePredicate predicate) {
        final GammaRefTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);
        boolean abort = true;
        try {
            if (!predicate.evaluate(longAsDouble(tranlocal.long_value))) {
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
        return String.format("GammaDoubleRef{orec=%s, version=%s, value=%s, hasListeners=%s)",
                ___toOrecString(), version, longAsDouble(long_value), listeners != null);
    }

    @Override
    public String toString() {
        return toString(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public String toString(Transaction tx) {
        return toString(asGammaTransaction(tx));
    }

    public String toString(GammaTransaction tx) {
        return Double.toString(get(tx));
    }

    @Override
    public String atomicToString() {
        return Double.toString(atomicGet());
    }
}
