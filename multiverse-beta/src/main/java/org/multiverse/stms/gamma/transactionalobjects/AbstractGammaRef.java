package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.*;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactions.*;

import static org.multiverse.stms.gamma.ThreadLocalGammaObjectPool.getThreadLocalGammaObjectPool;

public abstract class AbstractGammaRef extends AbstractGammaObject {

    private final int type;
    @SuppressWarnings({"VolatileLongOrDoubleField"})
    public volatile long long_value;
    public volatile Object ref_value;

    protected AbstractGammaRef(GammaStm stm, int type) {
        super(stm);
        this.type = type;
    }

    public final int getType() {
        return type;
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    public final boolean flattenCommute(final GammaTransaction tx, final GammaRefTranlocal tranlocal, final int lockMode) {
        final GammaTransactionConfiguration config = tx.config;

        if (!load(tranlocal, lockMode, config.spinCount, tx.arriveEnabled)) {
            return false;
        }

        tranlocal.setDirty(!config.dirtyCheck);
        tranlocal.mode = TRANLOCAL_WRITE;

        if (!tx.isReadConsistent(tranlocal)) {
            return false;
        }

        boolean abort = true;
        //evaluatingCommute = true;
        try {
            CallableNode node = tranlocal.headCallable;
            while (node != null) {
                evaluate(tranlocal, node.function);
                tx.pool.putCallableNode(node);
                node = node.next;
            }
            tranlocal.headCallable = null;

            abort = false;
        } finally {
            //evaluatingCommute = false;
            if (abort) {
                tx.abort();
            }
        }

        return true;
    }

    private void evaluate(final GammaRefTranlocal tranlocal, final Function function) {
        switch (type) {
            case TYPE_REF:
                tranlocal.ref_value = function.call(tranlocal.ref_value);
                break;
            case TYPE_INT:
                IntFunction intFunction = (IntFunction) function;
                tranlocal.long_value = intFunction.call((int) tranlocal.long_value);
                break;
            case TYPE_LONG:
                LongFunction longFunction = (LongFunction) function;
                tranlocal.long_value = longFunction.call(tranlocal.long_value);
                break;
            case TYPE_DOUBLE:
                DoubleFunction doubleFunction = (DoubleFunction) function;
                double doubleResult = doubleFunction.call(GammaDoubleRef.asDouble(tranlocal.long_value));
                tranlocal.long_value = GammaDoubleRef.asLong(doubleResult);
                break;
            case TYPE_BOOLEAN:
                BooleanFunction booleanFunction = (BooleanFunction) function;
                boolean booleanResult = booleanFunction.call(GammaBooleanRef.asBoolean(tranlocal.long_value));
                tranlocal.long_value = GammaBooleanRef.asLong(booleanResult);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public final Listeners safe(final GammaRefTranlocal tranlocal, final GammaObjectPool pool) {
        if (!tranlocal.isDirty) {
            releaseAfterReading(tranlocal, pool);
            return null;
        }

        if (type == TYPE_REF) {
            ref_value = tranlocal.ref_value;
            //we need to set them to null to prevent memory leaks.
            tranlocal.ref_value = null;
            tranlocal.ref_oldValue = null;
        } else {
            long_value = tranlocal.long_value;
        }

        version = tranlocal.version + 1;

        Listeners listenerAfterWrite = listeners;

        if (listenerAfterWrite != null) {
            listenerAfterWrite = ___removeListenersAfterWrite();
        }

        //todo: content of this method can be inlined here.
        releaseAfterUpdate(tranlocal, pool);
        return listenerAfterWrite;
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    public boolean prepare(final GammaTransaction tx, final GammaRefTranlocal tranlocal) {
        int mode = tranlocal.getMode();
        if (mode == TRANLOCAL_CONSTRUCTING) {
            return true;
        }

        if (mode == TRANLOCAL_READ) {
            return true;
        }

        if (mode == TRANLOCAL_COMMUTING) {
            if (!flattenCommute(tx, tranlocal, LOCKMODE_COMMIT)) {
                return false;
            }
        }

        if (!tranlocal.isDirty()) {
            boolean isDirty;
            if (type == TYPE_REF) {
                //noinspection ObjectEquality
                isDirty = tranlocal.ref_value != tranlocal.ref_oldValue;
            } else {
                isDirty = tranlocal.long_value != tranlocal.long_oldValue;
            }

            if (!isDirty) {
                return true;
            }

            tranlocal.setDirty(true);
        }

        return tryLockAndCheckConflict(tx.config.spinCount, tranlocal, LOCKMODE_COMMIT);
    }

    public final void releaseAfterFailure(final GammaRefTranlocal tranlocal, final GammaObjectPool pool) {
        if (type == TYPE_REF) {
            tranlocal.ref_value = null;
            tranlocal.ref_oldValue = null;
        }

        if (tranlocal.headCallable != null) {
            CallableNode node = tranlocal.headCallable;
            do {
                CallableNode next = node.next;
                pool.putCallableNode(node);
                node = next;
            } while (node != null);
            tranlocal.headCallable = null;
        }

        if (tranlocal.hasDepartObligation()) {
            if (tranlocal.getLockMode() != LOCKMODE_NONE) {
                departAfterFailureAndUnlock();
                tranlocal.setLockMode(LOCKMODE_NONE);
            } else {
                departAfterFailure();
            }
            tranlocal.setDepartObligation(false);
        } else if (tranlocal.getLockMode() != LOCKMODE_NONE) {
            unlockWhenUnregistered();
            tranlocal.setLockMode(LOCKMODE_NONE);
        }

        tranlocal.owner = null;
    }

    public final void releaseAfterUpdate(final GammaRefTranlocal tranlocal, final GammaObjectPool pool) {
        if (type == TYPE_REF) {
            tranlocal.ref_value = null;
            tranlocal.ref_oldValue = null;
        }


        departAfterUpdateAndUnlock();
        tranlocal.setLockMode(LOCKMODE_NONE);
        tranlocal.owner = null;
        tranlocal.setDepartObligation(false);
    }

    public final void releaseAfterReading(final GammaRefTranlocal tranlocal, final GammaObjectPool pool) {
        if (type == TYPE_REF) {
            tranlocal.ref_value = null;
            tranlocal.ref_oldValue = null;
        }

        if (tranlocal.hasDepartObligation()) {
            if (tranlocal.getLockMode() != LOCKMODE_NONE) {
                departAfterReadingAndUnlock();
                tranlocal.setLockMode(LOCKMODE_NONE);
            } else {
                departAfterReading();
            }
            tranlocal.setDepartObligation(false);
        } else if (tranlocal.getLockMode() != LOCKMODE_NONE) {
            unlockWhenUnregistered();
            tranlocal.setLockMode(LOCKMODE_NONE);
        }

        tranlocal.owner = null;
    }

    public final boolean load(final GammaRefTranlocal tranlocal, final int lockMode, int spinCount, final boolean arriveNeeded) {
        if (lockMode == LOCKMODE_NONE) {
            while (true) {
                //JMM: nothing can jump behind the following statement
                long readLong = 0;
                Object readRef = null;
                if (type == TYPE_REF) {
                    readRef = ref_value;
                } else {
                    readLong = long_value;
                }
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

                if (type == TYPE_REF) {
                    //noinspection ObjectEquality
                    if (readVersion == version && readRef == ref_value) {
                        //at this point we are sure that the read was unlocked.
                        tranlocal.owner = this;
                        tranlocal.version = readVersion;
                        tranlocal.ref_value = readRef;
                        tranlocal.ref_oldValue = readRef;
                        tranlocal.setLockMode(LOCKMODE_NONE);
                        tranlocal.setDepartObligation(arriveStatus == ARRIVE_NORMAL);
                        return true;
                    }
                } else {
                    if (readVersion == version && readLong == long_value) {
                        //at this point we are sure that the read was unlocked.
                        tranlocal.owner = this;
                        tranlocal.version = readVersion;
                        tranlocal.long_value = readLong;
                        tranlocal.long_oldValue = readLong;
                        tranlocal.setLockMode(LOCKMODE_NONE);
                        tranlocal.setDepartObligation(arriveStatus == ARRIVE_NORMAL);
                        return true;
                    }
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
            if (type == TYPE_REF) {
                final Object v = ref_value;
                tranlocal.ref_value = v;
                tranlocal.ref_oldValue = v;
            } else {
                final long v = long_value;
                tranlocal.long_value = v;
                tranlocal.long_oldValue = v;
            }
            tranlocal.setLockMode(lockMode);
            tranlocal.setDepartObligation(arriveStatus == ARRIVE_NORMAL);
            return true;
        }
    }

    @Override
    public final GammaRefTranlocal openForConstruction(GammaTransaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx instanceof MonoGammaTransaction) {
            return openForConstruction((MonoGammaTransaction) tx);
        } else if (tx instanceof ArrayGammaTransaction) {
            return openForConstruction((ArrayGammaTransaction) tx);
        } else {
            return openForConstruction((MapGammaTransaction) tx);
        }
    }

    @Override
    public final GammaRefTranlocal openForConstruction(MonoGammaTransaction tx) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForConstructionOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForConstructionOnBadStm(this);
        }

        if (config.readonly) {
            throw tx.abortOpenForConstructionOnReadonly(this);
        }

        throw new TodoException();
    }

    @Override
    public final GammaRefTranlocal openForConstruction(MapGammaTransaction tx) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForConstructionOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForConstructionOnBadStm(this);
        }

        if (config.readonly) {
            throw tx.abortOpenForConstructionOnReadonly(this);
        }

        throw new TodoException();
    }

    @Override
    public final GammaRefTranlocal openForConstruction(ArrayGammaTransaction tx) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForConstructionOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForConstructionOnBadStm(this);
        }

        if (config.readonly) {
            throw tx.abortOpenForConstructionOnReadonly(this);
        }

        throw new TodoException();
    }

    @Override
    public final GammaRefTranlocal openForRead(final GammaTransaction tx, final int lockMode) {
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
    public final GammaRefTranlocal openForRead(final MonoGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForReadOnBadStm(this);
        }

        lockMode = config.readLockModeAsInt <= lockMode ? lockMode : config.readLockModeAsInt;

        final GammaRefTranlocal tranlocal = tx.tranlocal;

        //noinspection ObjectEquality
        if (tranlocal.owner == this) {
            if (tranlocal.isCommuting()) {
                if (!flattenCommute(tx, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }

                return tranlocal;
            }

            if (lockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            return tranlocal;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTooSmallSize(2);
        }

        if (!load(tranlocal, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        tranlocal.isDirty = false;
        tranlocal.mode = TRANLOCAL_READ;
        tranlocal.writeSkewCheck = false;
        return tranlocal;
    }

    @Override
    public final GammaRefTranlocal openForRead(final ArrayGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForReadOnBadStm(this);
        }

        GammaRefTranlocal found = null;
        GammaRefTranlocal newNode = null;
        GammaRefTranlocal node = tx.head;
        while (true) {
            if (node == null) {
                break;
            } else //noinspection ObjectEquality
                if (node.owner == this) {
                    found = node;
                    break;
                } else if (node.owner == null) {
                    newNode = node;
                    break;
                } else {
                    node = node.next;
                }
        }

        lockMode = config.readLockModeAsInt <= lockMode ? lockMode : config.readLockModeAsInt;

        if (found != null) {
            if (found.isCommuting()) {
                if (!flattenCommute(tx, found, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }

                return found;
            }

            if (lockMode > found.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, found, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            tx.shiftInFront(found);
            return found;
        }

        if (newNode == null) {
            throw tx.abortOnTooSmallSize(config.arrayTransactionSize + 1);
        }

        newNode.mode = TRANLOCAL_READ;
        newNode.isDirty = false;
        newNode.writeSkewCheck = false;

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
    public final GammaRefTranlocal openForRead(final MapGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForReadOnBadStm(this);
        }

        lockMode = config.readLockModeAsInt <= lockMode ? lockMode : config.readLockModeAsInt;

        final int identityHash = identityHashCode();
        final int indexOf = tx.indexOf(this, identityHash);

        if (indexOf > -1) {
            final GammaRefTranlocal tranlocal = tx.array[indexOf];

            if (tranlocal.isCommuting()) {
                if (!flattenCommute(tx, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }

                return tranlocal;
            }

            if (lockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            return tranlocal;
        }

        final GammaRefTranlocal tranlocal = tx.pool.take(this);
        tranlocal.mode = TRANLOCAL_READ;
        tranlocal.isDirty = false;
        tranlocal.writeSkewCheck = false;
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
    public final GammaRefTranlocal openForWrite(final GammaTransaction tx, final int lockMode) {
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
    public final GammaRefTranlocal openForWrite(final MapGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForWriteOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForWriteOnBadStm(this);
        }

        if (config.readonly) {
            throw tx.abortOpenForWriteOnReadonly(this);
        }

        lockMode = config.writeLockModeAsInt <= lockMode ? lockMode : config.writeLockModeAsInt;

        final int identityHash = identityHashCode();

        final int indexOf = tx.indexOf(this, identityHash);
        if (indexOf > -1) {
            GammaRefTranlocal tranlocal = tx.array[indexOf];

            if (tranlocal.isCommuting()) {
                if (!flattenCommute(tx, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
                return tranlocal;
            }

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

        final GammaRefTranlocal tranlocal = tx.pool.take(this);
        tx.attach(tranlocal, identityHash);
        tx.size++;
        tx.hasWrites = true;

        if (!load(tranlocal, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        tranlocal.setDirty(!config.dirtyCheck);
        tranlocal.mode = TRANLOCAL_WRITE;
        tranlocal.writeSkewCheck = false;

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
    public final GammaRefTranlocal openForWrite(final MonoGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForWriteOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForWriteOnBadStm(this);
        }

        if (config.readonly) {
            throw tx.abortOpenForWriteOnReadonly(this);
        }

        lockMode = config.writeLockModeAsInt <= lockMode ? lockMode : config.writeLockModeAsInt;

        final GammaRefTranlocal tranlocal = tx.tranlocal;

        //noinspection ObjectEquality
        if (tranlocal.owner == this) {
            if (tranlocal.isCommuting()) {
                if (!flattenCommute(tx, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
                return tranlocal;
            }

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
            throw tx.abortOnTooSmallSize(2);
        }

        if (!load(tranlocal, lockMode, config.spinCount, tx.arriveEnabled)) {
            throw tx.abortOnReadWriteConflict();
        }

        tranlocal.setDirty(!config.dirtyCheck);
        tranlocal.writeSkewCheck = false;
        tranlocal.mode = TRANLOCAL_WRITE;
        tx.hasWrites = true;
        return tranlocal;
    }

    @Override
    public final GammaRefTranlocal openForWrite(final ArrayGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForWriteOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForWriteOnBadStm(this);
        }

        if (config.readonly) {
            throw tx.abortOpenForWriteOnReadonly(this);
        }

        GammaRefTranlocal found = null;
        GammaRefTranlocal newNode = null;
        GammaRefTranlocal node = tx.head;
        while (true) {
            if (node == null) {
                break;
            } else //noinspection ObjectEquality
                if (node.owner == this) {
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
            tx.shiftInFront(found);

            if (found.isCommuting()) {
                if (!flattenCommute(tx, found, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
                return found;
            }

            if (lockMode > found.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, found, lockMode)) {
                    throw tx.abortOnReadWriteConflict();
                }
            }

            found.mode = TRANLOCAL_WRITE;
            found.setDirty(!config.dirtyCheck);
            tx.hasWrites = true;
            return found;
        }

        if (newNode == null) {
            throw tx.abortOnTooSmallSize(config.arrayTransactionSize + 1);
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
        newNode.writeSkewCheck = false;
        tx.needsConsistency = true;
        tx.hasWrites = true;
        tx.size++;
        tx.shiftInFront(newNode);
        return newNode;
    }

    public final void openForCommute(final Transaction tx, final Function function) {
        openForCommute((GammaTransaction) tx, function);
    }

    public final void openForCommute(final GammaTransaction tx, final Function function) {
        if (tx instanceof MonoGammaTransaction) {
            openForCommute((MonoGammaTransaction) tx, function);
        } else if (tx instanceof ArrayGammaTransaction) {
            openForCommute((ArrayGammaTransaction) tx, function);
        } else {
            openForCommute((MapGammaTransaction) tx, function);
        }
    }

    public final void openForCommute(final MonoGammaTransaction tx, final Function function) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortCommuteOnBadStatus(this, function);
        }

        if (function == null) {
            throw tx.abortCommuteOnNullFunction(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortCommuteOnBadStm(this);
        }

        if (config.isReadonly()) {
            throw tx.abortCommuteOnReadonly(this);
        }

        final GammaRefTranlocal tranlocal = tx.tranlocal;

        //noinspection ObjectEquality
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
                evaluate(tranlocal, function);
                abort = false;
            } finally {
                if (abort) {
                    tx.abort();
                }
            }
            return;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTooSmallSize(2);
        }

        tx.hasWrites = true;
        tranlocal.owner = this;
        tranlocal.mode = TRANLOCAL_COMMUTING;
        tranlocal.isDirty = !config.dirtyCheck;
        tranlocal.writeSkewCheck = false;
        tranlocal.addCommutingFunction(tx.pool, function);
    }

    public final void openForCommute(final ArrayGammaTransaction tx, final Function function) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortCommuteOnBadStatus(this, function);
        }

        if (function == null) {
            throw tx.abortCommuteOnNullFunction(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortCommuteOnBadStm(this);
        }

        if (config.isReadonly()) {
            throw tx.abortCommuteOnReadonly(this);
        }

        GammaRefTranlocal found = null;
        GammaRefTranlocal newNode = null;
        GammaRefTranlocal node = tx.head;
        while (true) {
            if (node == null) {
                break;
            } else //noinspection ObjectEquality
                if (node.owner == this) {
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

            //todo: write lock should be applied?
            if (found.isRead()) {
                found.mode = TRANLOCAL_WRITE;
                tx.hasWrites = true;
            }

            boolean abort = true;
            try {
                evaluate(found, function);
                abort = false;
            } finally {
                if (abort) {
                    tx.abort();
                }
            }
            return;
        }

        if (newNode == null) {
            throw tx.abortOnTooSmallSize(config.arrayTransactionSize + 1);
        }

        tx.size++;
        tx.shiftInFront(newNode);
        tx.hasWrites = true;
        newNode.mode = TRANLOCAL_COMMUTING;
        newNode.isDirty = !config.dirtyCheck;
        newNode.owner = this;
        newNode.writeSkewCheck = false;
        newNode.addCommutingFunction(tx.pool, function);
    }

    public final void openForCommute(final MapGammaTransaction tx, final Function function) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortCommuteOnBadStatus(this, function);
        }

        if (function == null) {
            throw tx.abortCommuteOnNullFunction(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortCommuteOnBadStm(this);
        }

        if (config.isReadonly()) {
            throw tx.abortCommuteOnReadonly(this);
        }

        final int identityHash = identityHashCode();
        final int indexOf = tx.indexOf(this, identityHash);

        if (indexOf > -1) {
            final GammaRefTranlocal tranlocal = tx.array[indexOf];
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
                evaluate(tranlocal, function);
                abort = false;
            } finally {
                if (abort) {
                    tx.abort();
                }
            }
            return;
        }

        final GammaRefTranlocal tranlocal = tx.pool.take(this);
        tranlocal.mode = TRANLOCAL_COMMUTING;
        tranlocal.writeSkewCheck = false;
        tx.hasWrites = true;
        tx.attach(tranlocal, identityHash);
        tx.size++;
        tranlocal.addCommutingFunction(tx.pool, function);
    }

    public final void ensure() {
        ensure(getRequiredThreadLocalGammaTransaction());
    }

    public final void ensure(final Transaction self) {
        ensure(asGammaTransaction(self));
    }

    public final void ensure(final GammaTransaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortEnsureOnBadStatus(this);
        }

        if (tx.config.readonly) {
            return;
        }

        GammaRefTranlocal tranlocal = openForRead(tx, LOCKMODE_NONE);
        tranlocal.writeSkewCheck = true;
    }

    public final long atomicGetLong() {
        assert type != TYPE_REF;

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

    public final Object atomicObjectGet() {
        assert type == TYPE_REF;

        int attempt = 1;
        do {
            if (!hasCommitLock()) {

                Object read = ref_value;

                if (!hasCommitLock()) {
                    return read;
                }
            }
            stm.defaultBackoffPolicy.delayedUninterruptible(attempt);
            attempt++;
        } while (attempt <= stm.spinCount);

        throw new LockedException();
    }

    public final long atomicSetLong(final long newValue, boolean returnOld) {
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
        //noinspection NonAtomicOperationOnVolatileField
        version++;

        final Listeners listeners = ___removeListenersAfterWrite();

        departAfterUpdateAndUnlock();

        if (listeners != null) {
            final GammaObjectPool pool = getThreadLocalGammaObjectPool();
            listeners.openAll(pool);
        }

        return returnOld ? oldValue : newValue;
    }

    public final boolean atomicCompareAndSetLong(final long expectedValue, final long newValue) {
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
        //noinspection NonAtomicOperationOnVolatileField
        version++;
        final Listeners listeners = ___removeListenersAfterWrite();

        departAfterUpdateAndUnlock();

        if (listeners != null) {
            listeners.openAll(getThreadLocalGammaObjectPool());
        }

        return true;
    }
}
