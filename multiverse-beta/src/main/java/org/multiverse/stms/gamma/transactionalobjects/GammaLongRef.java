package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.predicates.LongPredicate;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.multiverse.stms.gamma.ThreadLocalGammaObjectPool.getThreadLocalGammaObjectPool;

public final class GammaLongRef extends AbstractGammaRef implements LongRef {

    public GammaLongRef(GammaStm stm) {
        this(stm, 0);
    }

    public GammaLongRef(GammaStm stm, long initialValue) {
        super(stm, TYPE_LONG);
        this.long_value = initialValue;
        this.version = VERSION_UNCOMMITTED + 1;
    }

    public GammaLongRef(GammaTransaction tx) {
        super(tx.config.stm, TYPE_LONG);
        openForConstruction(tx);
    }


    @Override
    public long getAndSet(final long value) {
        return getAndSet(getRequiredThreadLocalGammaTransaction(), value);
    }

    public long getAndSet(final Transaction tx, final long value) {
        return getAndSet(asGammaTransaction(tx), value);
    }

    public long getAndSet(final GammaTransaction tx, final long value) {
        final GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        final long oldValue = tranlocal.long_value;
        tranlocal.long_value = value;
        return oldValue;
    }

    @Override
    public long set(final long value) {
        return set(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public long set(final Transaction tx, final long value) {
        return set(asGammaTransaction(tx), value);
    }

    public long set(final GammaTransaction tx, final long value) {
        openForWrite(tx, LOCKMODE_NONE).long_value = value;
        return value;
    }

    @Override
    public long get() {
        return get(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public long get(final Transaction tx) {
        return get(asGammaTransaction(tx));
    }

    public long get(final GammaTransaction tx) {
        final GammaRefTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);
        return tranlocal.long_value;
    }

    @Override
    public long atomicGet() {
        int attempt = 1;
        do {
            if (!hasCommitLock()) {

                long read = long_value;

                if (!hasCommitLock()) {
                    return read;
                }
            }
            stm.defaultBackoffPolicy.delayedUninterruptible(attempt);
            attempt++;
        } while (attempt <= stm.spinCount);

        throw new LockedException();
    }

    @Override
    public long atomicWeakGet() {
        return long_value;
    }

    @Override
    public long atomicSet(final long newValue) {
        atomicGetAndSet(newValue);
        return newValue;
    }

    @Override
    public long atomicGetAndSet(final long newValue) {
        final int arriveStatus = arriveAndCommitLockOrBackoff();

        if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
            throw new LockedException();
        }

        final long oldValue = long_value;

        if (oldValue == newValue) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockWhenUnregistered();
            } else {
                departAfterReadingAndUnlock();
            }

            return newValue;
        }

        long_value = newValue;
        version++;

        final Listeners listeners = ___removeListenersAfterWrite();

        departAfterUpdateAndUnlock();

        if (listeners != null) {
            final GammaObjectPool pool = getThreadLocalGammaObjectPool();
            listeners.openAll(pool);
        }

        return oldValue;
    }

    @Override
    public void commute(final LongFunction function) {
        commute(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public long atomicAlterAndGet(final LongFunction function) {
        return atomicAlter(function, false);
    }

    private long atomicAlter(final LongFunction function, final boolean returnOld) {
        if (function == null) {
            throw new NullPointerException("Function can't be null");
        }

        final int arriveStatus = arriveAndCommitLockOrBackoff();

        if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
            throw new LockedException();
        }

        final long oldValue = long_value;
        long newValue;
        boolean abort = true;
        try {
            newValue = function.call(oldValue);
            abort = false;
        } finally {
            if (abort) {
                departAfterFailureAndUnlock();
            }
        }

        if (oldValue == newValue) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockWhenUnregistered();
            } else {
                departAfterReadingAndUnlock();
            }

            return oldValue;
        }

        long_value = newValue;
        version++;

        final Listeners listeners = ___removeListenersAfterWrite();

        departAfterUpdateAndUnlock();

        if (listeners != null) {
            listeners.openAll(getThreadLocalGammaObjectPool());
        }

        return returnOld ? oldValue : newValue;
    }

    @Override
    public long alterAndGet(final LongFunction function) {
        return alterAndGet(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public long alterAndGet(final Transaction tx, final LongFunction function) {
        return alterAndGet(asGammaTransaction(tx), function);
    }

    public long alterAndGet(final GammaTransaction tx, final LongFunction function) {
        if (function == null) {
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        final GammaRefTranlocal write = openForWrite(tx, LOCKMODE_NONE);

        boolean abort = true;
        try {
            write.long_value = function.call(write.long_value);
            abort = false;
        } finally {
            if (abort) {
                tx.abort();
            }
        }
        return write.long_value;
    }

    @Override
    public long atomicGetAndAlter(final LongFunction function) {
        return atomicAlter(function, true);
    }

    @Override
    public long getAndAlter(final LongFunction function) {
        return getAndAlter(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public long getAndAlter(final Transaction tx, final LongFunction function) {
        return getAndAlter(asGammaTransaction(tx), function);
    }

    public long getAndAlter(final GammaTransaction tx, final LongFunction function) {
        if (function == null) {
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        final GammaRefTranlocal write = openForWrite(tx, LOCKMODE_NONE);

        final long oldValue = write.long_value;
        boolean abort = true;
        try {
            write.long_value = function.call(write.long_value);
            abort = false;
        } finally {
            if (abort) {
                tx.abort();
            }
        }
        return oldValue;
    }

    @Override
    public boolean atomicCompareAndSet(final long expectedValue, final long newValue) {
        final int arriveStatus = arriveAndCommitLockOrBackoff();

        if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
            throw new LockedException();
        }

        final long currentValue = long_value;

        if (currentValue != expectedValue) {
            departAfterFailureAndUnlock();
            return false;
        }

        if (expectedValue == newValue) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockWhenUnregistered();
            } else {
                departAfterReadingAndUnlock();
            }

            return true;
        }

        long_value = newValue;
        version++;
        final Listeners listeners = ___removeListenersAfterWrite();

        departAfterUpdateAndUnlock();

        if (listeners != null) {
            listeners.openAll(getThreadLocalGammaObjectPool());
        }

        return true;
    }

    @Override
    public long atomicGetAndIncrement(final long amount) {
        final long result = atomicIncrementAndGet(amount);
        return result - amount;
    }

    @Override
    public long getAndIncrement(final long amount) {
        return getAndIncrement(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public long getAndIncrement(final Transaction tx, final long amount) {
        return getAndIncrement((GammaTransaction) tx, amount);
    }

    public long getAndIncrement(final GammaTransaction tx, final long amount) {
        final GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        final long oldValue = tranlocal.long_value;
        tranlocal.long_value += amount;
        return oldValue;
    }

    @Override
    public long atomicIncrementAndGet(final long amount) {
        final int arriveStatus = arriveAndCommitLockOrBackoff();

        if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
            throw new LockedException();
        }

        final long oldValue = long_value;

        if (amount == 0) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockWhenUnregistered();
            } else {
                departAfterReadingAndUnlock();
            }

            return oldValue;
        }

        final long newValue = oldValue + amount;
        long_value = newValue;
        version++;

        final Listeners listeners = ___removeListenersAfterWrite();

        departAfterUpdateAndUnlock();

        if (listeners != null) {
            listeners.openAll(getThreadLocalGammaObjectPool());
        }

        return newValue;
    }

    @Override
    public long incrementAndGet(final long amount) {
        return incrementAndGet(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public long incrementAndGet(final Transaction tx, final long amount) {
        return incrementAndGet(asGammaTransaction(tx), amount);
    }

    public long incrementAndGet(final GammaTransaction tx, final long amount) {
        final GammaRefTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value += amount;
        return tranlocal.long_value;
    }

    @Override
    public void increment() {
        increment(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public void increment(final Transaction tx) {
        commute(asGammaTransaction(tx), Functions.newIncLongFunction());
    }

    public void increment(final GammaTransaction tx) {
        commute(tx, Functions.newIncLongFunction());
    }

    @Override
    public void increment(final long amount) {
        commute(getRequiredThreadLocalGammaTransaction(), Functions.newIncLongFunction(amount));
    }

    @Override
    public void increment(final Transaction tx, final long amount) {
        commute(asGammaTransaction(tx), Functions.newIncLongFunction(amount));
    }

    @Override
    public void decrement() {
        commute(getRequiredThreadLocalGammaTransaction(), Functions.newDecLongFunction());
    }

    @Override
    public void decrement(final Transaction tx) {
        commute(asGammaTransaction(tx), Functions.newDecLongFunction());
    }

    @Override
    public void decrement(final long amount) {
        commute(getRequiredThreadLocalGammaTransaction(), Functions.newIncLongFunction(-amount));
    }

    @Override
    public void decrement(final Transaction tx, final long amount) {
        commute(asGammaTransaction(tx), Functions.newIncLongFunction(-amount));
    }

    @Override
    public void await(final long value) {
        await(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public void await(final Transaction tx, final long value) {
        await(asGammaTransaction(tx), value);
    }

    public void await(final GammaTransaction tx, final long value) {
        GammaRefTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);
        if (value == tranlocal.long_value) {
            return;
        }

        tx.retry();
    }

    @Override
    public void await(final LongPredicate predicate) {
        await(getRequiredThreadLocalGammaTransaction(), predicate);
    }

    @Override
    public void await(final Transaction tx, final LongPredicate predicate) {
        await(asGammaTransaction(tx), predicate);
    }

    public void await(final GammaTransaction tx, final LongPredicate predicate) {
        GammaRefTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);
        boolean abort = true;
        try {
            if (!predicate.evaluate(tranlocal.long_value)) {
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
        return String.format("GammaLongRef{orec=%s, version=%s, value=%s, hasListeners=%s)",
                ___toOrecString(), version, long_value, listeners != null);
    }
}
