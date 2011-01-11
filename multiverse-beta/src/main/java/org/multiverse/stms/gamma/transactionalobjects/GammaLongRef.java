package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.predicates.LongPredicate;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactions.*;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.gamma.ThreadLocalGammaObjectPool.getThreadLocalGammaObjectPool;

public class GammaLongRef extends AbstractGammaObject implements LongRef {

    public volatile long value;

    public GammaLongRef(GammaStm stm) {
        this(stm, 0);
    }

    public GammaLongRef(GammaStm stm, long initialValue) {
        super(stm);
        this.value = initialValue;
        this.version = VERSION_UNCOMMITTED + 1;
    }

    public GammaLongRef(GammaTransaction tx) {
        super(tx.config.stm);
        tx.openForConstruction(this);
    }

    public boolean load(final GammaTranlocal tranlocal, final int lockMode, int spinCount, final boolean arriveNeeded) {
        if (lockMode == LOCKMODE_NONE) {
            while (true) {
                //JMM: nothing can jump behind the following statement
                final long readValue = value;
                final long readVersion = version;

                //JMM: the read for the arrive can't jump over the read of the active.

                int arriveStatus;
                if (arriveNeeded) {
                    arriveStatus = arrive(spinCount);
                } else {
                    arriveStatus = waitForNoCommitLock(spinCount) ? ARRIVE_UNREGISTERED : ARRIVE_LOCK_NOT_FREE;
                }

                if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                    return false;
                }

                //JMM safety:
                //The volatile read of active can't be reordered so that it jump in front of the volatile read of
                //the orec-value when the arrive method is called.
                //An instruction is allowed to jump in front of the write of orec-value, but it is not allowed to
                //jump in front of the read or orec-value (volatile read happens before rule).
                //This means that it isn't possible that a locked value illegally is seen as unlocked.

                if (readVersion == version && readValue == value) {
                    //at this point we are sure that the read was unlocked.
                    tranlocal.owner = this;
                    tranlocal.version = readVersion;
                    tranlocal.long_value = readValue;
                    tranlocal.long_oldValue = readValue;
                    tranlocal.setLockMode(LOCKMODE_NONE);
                    tranlocal.setDepartObligation(arriveStatus == ARRIVE_NORMAL);
                    return true;
                }

                //we are not lucky, the value has changed. But before retrying, we need to depart if the arrive was normal
                if (arriveStatus == ARRIVE_NORMAL) {
                    departAfterFailure();
                }
            }
        } else {
            final int arriveStatus = tryLockAndArrive(spinCount, lockMode);

            if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                return false;
            }

            tranlocal.owner = this;
            tranlocal.version = version;
            final long v = value;
            tranlocal.long_value = v;
            tranlocal.long_oldValue = v;
            tranlocal.setLockMode(lockMode);
            tranlocal.setDepartObligation(arriveStatus == ARRIVE_NORMAL);
            return true;
        }
    }

    @Override
    public GammaTranlocal openForWrite(final GammaTransaction tx, final int lockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx instanceof MonoGammaTransaction) {
            return openForWrite((MonoGammaTransaction) tx, lockMode);
        } else if (tx instanceof ArrayGammaTransaction) {
            return openForWrite((ArrayGammaTransaction) tx, lockMode);
        } else {
            return openForWrite((MapGammaTransaction) tx, lockMode);
        }
    }

    @Override
    public GammaTranlocal openForWrite(final MapGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForWriteOnBadStatus();
        }

        final GammaTransactionConfiguration config = tx.config;

        if (config.readonly) {
            throw tx.abortOpenForWriteOnReadonly();
        }

        lockMode = config.writeLockModeAsInt <= lockMode ? lockMode : config.writeLockModeAsInt;

        final int identityHash = identityHashCode();

        final int indexOf = tx.indexOf(this, identityHash);
        if (indexOf > -1) {
            GammaTranlocal tranlocal = tx.array[indexOf];

            if (lockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            tx.hasWrites = true;
            tranlocal.setDirty(!config.dirtyCheck);
            tranlocal.mode = TRANLOCAL_WRITE;
            return tranlocal;
        }

        final GammaTranlocal tranlocal = tx.pool.take(this);
        tx.attach(tranlocal, identityHash);
        tx.size++;
        tx.hasWrites = true;

        if (!load(tranlocal, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        tranlocal.setDirty(!config.dirtyCheck);
        tranlocal.mode = TRANLOCAL_WRITE;

        if (tx.needsConsistency) {
            if (!tx.isReadConsistent(tranlocal)) {
                throw tx.abortOnReadWriteConflict();
            }
        } else {
            tx.needsConsistency = true;
        }

        return tranlocal;
    }

    @Override
    public GammaTranlocal openForWrite(final MonoGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForWriteOnBadStatus();
        }

        final GammaTransactionConfiguration config = tx.config;

        if (config.readonly) {
            throw tx.abortOpenForWriteOnReadonly();
        }

        lockMode = config.writeLockModeAsInt <= lockMode ? lockMode : config.writeLockModeAsInt;

        final GammaTranlocal tranlocal = tx.tranlocal;

        if (tranlocal.owner == this) {
            if (lockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            tx.hasWrites = true;
            tranlocal.setDirty(!config.dirtyCheck);
            tranlocal.mode = TRANLOCAL_WRITE;
            return tranlocal;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTransactionTooSmall();
        }

        if (!load(tranlocal, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        tranlocal.setDirty(!config.dirtyCheck);
        tranlocal.mode = TRANLOCAL_WRITE;
        tx.hasWrites = true;
        return tranlocal;
    }

    @Override
    public GammaTranlocal openForWrite(final ArrayGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForWriteOnBadStatus();
        }

        final GammaTransactionConfiguration config = tx.config;
        if (config.readonly) {
            throw tx.abortOpenForWriteOnReadonly();
        }

        GammaTranlocal found = null;
        GammaTranlocal newNode = null;
        GammaTranlocal node = tx.head;
        while (true) {
            if (node == null) {
                break;
            } else if (node.owner == this) {
                found = node;
                break;
            } else if (node.owner == null) {
                newNode = node;
                break;
            } else {
                node = node.next;
            }
        }

        lockMode = config.writeLockModeAsInt > lockMode ? config.writeLockModeAsInt : lockMode;

        if (found != null) {
            if (lockMode > found.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, found, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            found.mode = TRANLOCAL_WRITE;
            found.setDirty(!config.dirtyCheck);
            tx.hasWrites = true;
            tx.shiftInFront(found);
            return found;
        }

        if (newNode == null) {
            throw tx.abortOnTransactionTooSmall();
        }

        if (!load(newNode, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        if (tx.needsConsistency) {
            if (!tx.isReadConsistent(newNode)) {
                throw tx.abortOnReadWriteConflict();
            }
        } else {
            tx.needsConsistency = true;
        }

        newNode.mode = TRANLOCAL_WRITE;
        newNode.setDirty(!config.dirtyCheck);
        tx.needsConsistency = true;
        tx.hasWrites = true;
        tx.size++;
        tx.shiftInFront(newNode);
        return newNode;
    }

    @Override
    public GammaTranlocal openForRead(final GammaTransaction tx, final int lockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx instanceof MonoGammaTransaction) {
            return openForRead((MonoGammaTransaction) tx, lockMode);
        } else if (tx instanceof ArrayGammaTransaction) {
            return openForRead((ArrayGammaTransaction) tx, lockMode);
        } else {
            return openForRead((MapGammaTransaction) tx, lockMode);
        }
    }

    @Override
    public GammaTranlocal openForRead(final MonoGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus();
        }

        final GammaTransactionConfiguration config = tx.config;

        lockMode = config.readLockModeAsInt <= lockMode ? lockMode : config.readLockModeAsInt;

        final GammaTranlocal tranlocal = tx.tranlocal;

        if (tranlocal.owner == this) {
            if (lockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            return tranlocal;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTransactionTooSmall();
        }

        if (!load(tranlocal, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        tranlocal.mode = TRANLOCAL_READ;
        return tranlocal;
    }

    @Override
    public GammaTranlocal openForRead(final ArrayGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus();
        }

        GammaTranlocal found = null;
        GammaTranlocal newNode = null;
        GammaTranlocal node = tx.head;
        while (true) {
            if (node == null) {
                break;
            } else if (node.owner == this) {
                found = node;
                break;
            } else if (node.owner == null) {
                newNode = node;
                break;
            } else {
                node = node.next;
            }
        }

        final GammaTransactionConfiguration config = tx.config;

        lockMode = config.readLockModeAsInt <= lockMode ? lockMode : config.readLockModeAsInt;

        if (found != null) {
            if (lockMode > found.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, found, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            tx.shiftInFront(found);
            return found;
        }

        if (newNode == null) {
            throw tx.abortOnTransactionTooSmall();
        }

        newNode.mode = TRANLOCAL_READ;

        if (!load(newNode, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        tx.size++;
        tx.shiftInFront(newNode);

        if (tx.needsConsistency) {
            if (!tx.isReadConsistent(newNode)) {
                throw tx.abortOnReadWriteConflict();
            }
        } else {
            tx.needsConsistency = true;
        }

        return newNode;
    }

    @Override
    public GammaTranlocal openForRead(final MapGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus();
        }

        final GammaTransactionConfiguration config = tx.config;

        lockMode = config.readLockModeAsInt <= lockMode ? lockMode : config.readLockModeAsInt;

        final int identityHash = identityHashCode();
        final int indexOf = tx.indexOf(this, identityHash);

        if (indexOf > -1) {
            final GammaTranlocal tranlocal = tx.array[indexOf];

            if (lockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            return tranlocal;
        }

        final GammaTranlocal tranlocal = tx.pool.take(this);
        tranlocal.mode = TRANLOCAL_READ;
        tx.attach(tranlocal, identityHash);
        tx.size++;

        if (!load(tranlocal, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        if (tx.needsConsistency) {
            if (!tx.isReadConsistent(tranlocal)) {
                throw tx.abortOnReadWriteConflict();
            }
        } else {
            tx.needsConsistency = true;
        }

        return tranlocal;
    }

    @Override
    public void commute(final Transaction tx, final LongFunction function) {
        commute((GammaTransaction) tx, function);
    }

    public void commute(final GammaTransaction tx, final LongFunction function) {
        if (tx instanceof MonoGammaTransaction) {
            commute((MonoGammaTransaction) tx, function);
        } else if (tx instanceof ArrayGammaTransaction) {
            commute((ArrayGammaTransaction) tx, function);
        } else {
            commute((MapGammaTransaction) tx, function);
        }
    }

    public void commute(final MonoGammaTransaction tx, final LongFunction function) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortCommuteOnBadStatus();
        }

        if (function == null) {
            throw tx.abortCommuteOnBadStatus();
        }

        final GammaTransactionConfiguration config = tx.config;
        if (config.isReadonly()) {
            throw tx.abortCommuteOnReadonly();
        }

        final GammaTranlocal tranlocal = tx.tranlocal;

        if (tranlocal.owner == this) {
            if (tranlocal.isCommuting()) {
                tranlocal.addCommutingFunction(tx.pool, function);
                return;
            }

            if (tranlocal.isRead()) {
                tranlocal.mode = TRANLOCAL_WRITE;
                tx.hasWrites = true;
            }

            boolean abort = true;
            try {
                tranlocal.long_value = function.call(tranlocal.long_value);
                abort = false;
            } finally {
                if (abort) {
                    tx.abort();
                }
            }
            return;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTransactionTooSmall();
        }

        tx.hasWrites = true;
        tranlocal.mode = TRANLOCAL_COMMUTING;
        tranlocal.isDirty = !config.dirtyCheck;
        tranlocal.addCommutingFunction(tx.pool, function);
    }

    public void commute(final ArrayGammaTransaction tx, final LongFunction function) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortCommuteOnBadStatus();
        }

        if (function == null) {
            throw tx.abortCommuteOnNullFunction();
        }

        final GammaTransactionConfiguration config = tx.config;
        if (config.isReadonly()) {
            throw tx.abortCommuteOnReadonly();
        }

        GammaTranlocal found = null;
        GammaTranlocal newNode = null;
        GammaTranlocal node = tx.head;
        while (true) {
            if (node == null) {
                break;
            } else if (node.owner == this) {
                found = node;
                break;
            } else if (node.owner == null) {
                newNode = node;
                break;
            } else {
                node = node.next;
            }
        }

        if (found != null) {
            if (found.isCommuting()) {
                found.addCommutingFunction(tx.pool, function);
                return;
            }

            if (found.isRead()) {
                found.mode = TRANLOCAL_WRITE;
                tx.hasWrites = true;
            }

            boolean abort = true;
            try {
                found.long_value = function.call(found.long_value);
                abort = false;
            } finally {
                if (abort) {
                    tx.abort();
                }
            }
            return;
        }

        if (newNode == null) {
            throw tx.abortOnTransactionTooSmall();
        }

        tx.size++;
        tx.shiftInFront(newNode);
        tx.hasWrites = true;
        newNode.mode = TRANLOCAL_COMMUTING;
        newNode.isDirty = !config.dirtyCheck;
        newNode.addCommutingFunction(tx.pool, function);
    }

    public void commute(final MapGammaTransaction tx, final LongFunction function) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortCommuteOnBadStatus();
        }

        if (function == null) {
            throw tx.abortCommuteOnNullFunction();
        }

        final GammaTransactionConfiguration config = tx.config;
        if (config.isReadonly()) {
            throw tx.abortCommuteOnReadonly();
        }

        final int identityHash = identityHashCode();
        final int indexOf = tx.indexOf(this, identityHash);

        if (indexOf > -1) {
            final GammaTranlocal tranlocal = tx.array[indexOf];
            if (tranlocal.isCommuting()) {
                tranlocal.addCommutingFunction(tx.pool, function);
                return;
            }

            if (tranlocal.isRead()) {
                tranlocal.mode = TRANLOCAL_WRITE;
                tx.hasWrites = true;
            }

            boolean abort = true;
            try {
                tranlocal.long_value = function.call(tranlocal.long_value);
                abort = false;
            } finally {
                if (abort) {
                    tx.abort();
                }
            }
            return;
        }

        final GammaTranlocal tranlocal = tx.pool.take(this);
        tranlocal.mode = TRANLOCAL_COMMUTING;
        tx.hasWrites = true;
        tx.attach(tranlocal, identityHash);
        tx.size++;
        tranlocal.addCommutingFunction(tx.pool, function);
    }

    @Override
    public long getAndSet(final long value) {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return getAndSet(tx, value);
    }

    public long getAndSet(final Transaction tx, final long value) {
        return getAndSet((GammaTransaction) tx, value);
    }

    public long getAndSet(final GammaTransaction tx, final long value) {
        final GammaTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        final long oldValue = tranlocal.long_value;
        tranlocal.long_value = value;
        return oldValue;
    }

    @Override
    public long set(final long value) {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return set(tx, value);
    }

    @Override
    public long set(final Transaction tx, final long value) {
        return set((GammaTransaction) tx, value);
    }

    public long set(final GammaTransaction tx, final long value) {
        openForWrite(tx, LOCKMODE_NONE).long_value = value;
        return value;
    }

    @Override
    public long get() {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return get(tx);
    }

    @Override
    public long get(final Transaction tx) {
        return get((GammaTransaction) tx);
    }

    public long get(final GammaTransaction tx) {
        final GammaTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);
        return tranlocal.long_value;
    }

    @Override
    public long atomicGet() {
        int attempt = 1;
        do {
            if (!hasCommitLock()) {

                long read = value;

                if (!hasCommitLock()) {
                    return read;
                }
            }
            ___stm.defaultBackoffPolicy.delayedUninterruptible(attempt);
            attempt++;
        } while (attempt <= ___stm.spinCount);

        throw new LockedException();
    }

    @Override
    public long atomicWeakGet() {
        return value;
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

        final long oldValue = value;

        if (oldValue == newValue) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockByReadBiased();
            } else {
                departAfterReadingAndUnlock();
            }

            return newValue;
        }

        value = newValue;
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
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        commute(tx, function);
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

        final long oldValue = value;
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
                unlockByReadBiased();
            } else {
                departAfterReadingAndUnlock();
            }

            return oldValue;
        }

        value = newValue;
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
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return alterAndGet(tx, function);
    }

    @Override
    public long alterAndGet(final Transaction tx, final LongFunction function) {
        return alterAndGet((GammaTransaction) tx, function);
    }

    public long alterAndGet(final GammaTransaction tx, final LongFunction function) {
        if (function == null) {
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        final GammaTranlocal write = openForWrite(tx, LOCKMODE_NONE);

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
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return getAndAlter(tx, function);
    }

    @Override
    public long getAndAlter(final Transaction tx, final LongFunction function) {
        return getAndAlter((GammaTransaction) tx, function);
    }

    public long getAndAlter(final GammaTransaction tx, final LongFunction function) {
        if (function == null) {
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        final GammaTranlocal write = openForWrite(tx, LOCKMODE_NONE);

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

        final long currentValue = value;

        if (currentValue != expectedValue) {
            departAfterFailureAndUnlock();
            return false;
        }

        if (expectedValue == newValue) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockByReadBiased();
            } else {
                departAfterReadingAndUnlock();
            }

            return true;
        }

        value = newValue;
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
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return getAndIncrement(tx, amount);
    }

    @Override
    public long getAndIncrement(final Transaction tx, final long amount) {
        return getAndIncrement((GammaTransaction) tx, amount);
    }

    public long getAndIncrement(final GammaTransaction tx, final long amount) {
        final GammaTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
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

        final long oldValue = value;

        if (amount == 0) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockByReadBiased();
            } else {
                departAfterReadingAndUnlock();
            }

            return oldValue;
        }

        final long newValue = oldValue + amount;
        value = newValue;
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
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return incrementAndGet(tx, amount);
    }

    @Override
    public long incrementAndGet(final Transaction tx, final long amount) {
        return incrementAndGet((GammaTransaction) tx, amount);
    }

    public long incrementAndGet(final GammaTransaction tx, final long amount) {
        final GammaTranlocal tranlocal = openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value += amount;
        return tranlocal.long_value;
    }

    @Override
    public void increment() {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        increment(tx);
    }

    @Override
    public void increment(final Transaction tx) {
        commute((GammaTransaction) tx, Functions.newIncLongFunction());
    }

    public void increment(final GammaTransaction tx) {
        commute(tx, Functions.newIncLongFunction());
    }

    @Override
    public void increment(final long amount) {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        commute(tx, Functions.newIncLongFunction(amount));
    }

    @Override
    public void increment(final Transaction tx, final long amount) {
        commute((GammaTransaction) tx, Functions.newIncLongFunction(amount));
    }

    @Override
    public void decrement() {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        commute(tx, Functions.newDecLongFunction());
    }

    @Override
    public void decrement(final Transaction tx) {
        commute((GammaTransaction) tx, Functions.newDecLongFunction());
    }

    @Override
    public void decrement(final long amount) {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        commute(tx, Functions.newIncLongFunction(-amount));
    }

    @Override
    public void decrement(final Transaction tx, final long amount) {
        commute((GammaTransaction) tx, Functions.newIncLongFunction(-amount));
    }

    @Override
    public void await(final long value) {
        //todo
        throw new TodoException();
    }

    @Override
    public void await(final Transaction tx, final long value) {
        //todo
        throw new TodoException();
    }

    @Override
    public void await(final LongPredicate predicate) {
        //todo
        throw new TodoException();
    }

    @Override
    public void await(final Transaction tx, final LongPredicate predicate) {
        //todo
        throw new TodoException();
    }

    @Override
    public void ensure() {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        ensure(tx);
    }

    @Override
    public void ensure(final Transaction tx) {
        ensure((GammaTransaction) tx);
    }

    public void ensure(final GammaTransaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortEnsureOnBadStatus();
        }

        //openForRead(tx, LOCKMODE_NONE);
        throw new TodoException();
    }

    @Override
    public String toDebugString() {
        return String.format("GammaLongRef{orec=%s, version=%s, value=%s, hasListeners=%s)",
                ___toOrecString(), version, value, ___listeners != null);
    }
}
