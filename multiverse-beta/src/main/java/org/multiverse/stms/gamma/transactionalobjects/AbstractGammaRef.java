package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.IsolationLevel;
import org.multiverse.api.LockMode;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.functions.*;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GammaStmUtils;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.fat.FatFixedLengthGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanFixedLengthGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanMonoGammaTransaction;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaStmUtils.asGammaTransaction;
import static org.multiverse.stms.gamma.GammaStmUtils.getRequiredThreadLocalGammaTransaction;
import static org.multiverse.stms.gamma.ThreadLocalGammaObjectPool.getThreadLocalGammaObjectPool;

@SuppressWarnings({"OverlyComplexClass", "OverlyCoupledClass"})
public abstract class AbstractGammaRef extends AbstractGammaObject {

    public final int type;
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

        if (!load(tranlocal, lockMode, config.spinCount, !tx.poorMansConflictScan)) {
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
                CallableNode newNext = node.next;
                tx.pool.putCallableNode(node);
                node = newNext;
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
                double doubleResult = doubleFunction.call(GammaStmUtils.longAsDouble(tranlocal.long_value));
                tranlocal.long_value = GammaStmUtils.doubleAsLong(doubleResult);
                break;
            case TYPE_BOOLEAN:
                BooleanFunction booleanFunction = (BooleanFunction) function;
                boolean booleanResult = booleanFunction.call(GammaStmUtils.longAsBoolean(tranlocal.long_value));
                tranlocal.long_value = GammaStmUtils.booleanAsLong(booleanResult);
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

    public final Listeners leanSafe(final GammaRefTranlocal tranlocal) {
        if (tranlocal.mode == TRANLOCAL_READ) {
            tranlocal.ref_value = null;
            tranlocal.owner = null;
            return null;
        }

        ref_value = tranlocal.ref_value;

        version = tranlocal.version + 1;

        Listeners listenerAfterWrite = listeners;

        if (listenerAfterWrite != null) {
            listenerAfterWrite = ___removeListenersAfterWrite();
        }

        departAfterUpdateAndUnlock();
        tranlocal.ref_value = null;
        tranlocal.lockMode = LOCKMODE_NONE;
        tranlocal.owner = null;
        tranlocal.hasDepartObligation = false;
        return listenerAfterWrite;
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    public final boolean prepare(final GammaTransaction tx, final GammaRefTranlocal tranlocal) {
        final int mode = tranlocal.getMode();

        if (mode == TRANLOCAL_CONSTRUCTING) {
            return true;
        }

        if (mode == TRANLOCAL_READ) {
            return !tranlocal.writeSkewCheck
                    || tryLockAndCheckConflict(tx.config.spinCount, tranlocal, LOCKMODE_READ);
        }

        if (mode == TRANLOCAL_COMMUTING) {
            if (!flattenCommute(tx, tranlocal, LOCKMODE_EXCLUSIVE)) {
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
                return !tranlocal.writeSkewCheck ||
                        tryLockAndCheckConflict(tx.config.spinCount, tranlocal, LOCKMODE_READ);
            }

            tranlocal.setDirty(true);
        }

        return tryLockAndCheckConflict(tx.config.spinCount, tranlocal, LOCKMODE_EXCLUSIVE);
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
            unlockByUnregistered();
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
            unlockByUnregistered();
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
                long readVersion;
                if (type == TYPE_REF) {
                    do {
                        readRef = ref_value;
                        readVersion = version;
                    } while (readRef != ref_value);
                } else {
                    do {
                        readLong = long_value;
                        readVersion = version;
                    } while (readLong != long_value);
                }

                int arriveStatus = arriveNeeded
                        ? arrive(spinCount)
                        : (waitForExclusiveLockToBecomeFree(spinCount) ? ARRIVE_UNREGISTERED : ARRIVE_LOCK_NOT_FREE);

                if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                    return false;
                }

                //noinspection ObjectEquality
                if (readVersion == version) {
                    tranlocal.owner = this;
                    tranlocal.version = readVersion;
                    tranlocal.setLockMode(LOCKMODE_NONE);
                    tranlocal.setDepartObligation(arriveStatus == ARRIVE_NORMAL);

                    if (type == TYPE_REF) {
                        tranlocal.ref_value = readRef;
                        tranlocal.ref_oldValue = readRef;
                    } else {
                        tranlocal.long_value = readLong;
                        tranlocal.long_oldValue = readLong;
                    }

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

    public final GammaRefTranlocal openForConstruction(GammaTransaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }


        final int type = tx.transactionType;

        if (type == TRANSACTIONTYPE_FAT_MONO) {
            return openForConstruction((FatMonoGammaTransaction) tx);
        } else if (type == TRANSACTIONTYPE_FAT_FIXED_LENGTH) {
            return openForConstruction((FatFixedLengthGammaTransaction) tx);
        } else if (type == TRANSACTIONTYPE_FAT_VARIABLE_LENGTH) {
            return openForConstruction((FatVariableLengthGammaTransaction) tx);
        } else {
            throw tx.abortOpenForConstructionRequired(this);
        }
    }

    private void initTranlocalForConstruction(final GammaRefTranlocal tranlocal) {
        tranlocal.isDirty = true;
        tranlocal.mode = TRANLOCAL_CONSTRUCTING;
        tranlocal.setLockMode(LOCKMODE_EXCLUSIVE);
        tranlocal.setDepartObligation(true);
        if (type == TYPE_REF) {
            tranlocal.ref_value = null;
            tranlocal.ref_oldValue = null;
        } else {
            tranlocal.long_value = 0;
            tranlocal.long_oldValue = 0;
        }
    }

    public final GammaRefTranlocal openForConstruction(FatMonoGammaTransaction tx) {
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

        final GammaRefTranlocal tranlocal = tx.tranlocal;

        //noinspection ObjectEquality
        if (tranlocal.owner == this) {
            if (!tranlocal.isConstructing()) {
                throw tx.abortOpenForConstructionOnBadReference(this);
            }

            return tranlocal;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTransactionTooSmall(2);
        }

        tx.hasWrites = true;
        tranlocal.owner = this;
        initTranlocalForConstruction(tranlocal);
        return tranlocal;
    }

    public final GammaRefTranlocal openForConstruction(FatVariableLengthGammaTransaction tx) {
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

        final int identityHash = identityHashCode();
        final int indexOf = tx.indexOf(this, identityHash);

        if (indexOf > -1) {
            final GammaRefTranlocal tranlocal = tx.array[indexOf];

            if (!tranlocal.isConstructing()) {
                throw tx.abortOpenForConstructionOnBadReference(this);
            }

            return tranlocal;
        }

        final GammaRefTranlocal tranlocal = tx.pool.take(this);
        tranlocal.owner = this;
        initTranlocalForConstruction(tranlocal);
        tx.hasWrites = true;
        tx.attach(tranlocal, identityHash);
        tx.size++;

        return tranlocal;
    }

    public final GammaRefTranlocal openForConstruction(FatFixedLengthGammaTransaction tx) {
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

        GammaRefTranlocal found = null;
        GammaRefTranlocal newNode = null;
        GammaRefTranlocal node = tx.head;
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
            if (!found.isConstructing()) {
                throw tx.abortOpenForConstructionOnBadReference(this);
            }

            tx.shiftInFront(found);
            return found;
        }

        if (newNode == null) {
            throw tx.abortOnTransactionTooSmall(config.maxFixedLengthTransactionSize + 1);
        }

        newNode.owner = this;
        initTranlocalForConstruction(newNode);
        tx.size++;
        tx.shiftInFront(newNode);
        tx.hasWrites = true;
        return newNode;
    }
    // ============================================================================================
    // =============================== open for read ==============================================
    // ============================================================================================

    public final GammaRefTranlocal openForRead(final GammaTransaction tx, final int lockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        final int type = tx.transactionType;

        if (type == TRANSACTIONTYPE_LEAN_MONO) {
            return openForRead((LeanMonoGammaTransaction) tx, lockMode);
        } else if (type == TRANSACTIONTYPE_LEAN_FIXED_LENGTH) {
            return openForRead((LeanFixedLengthGammaTransaction) tx, lockMode);
        } else if (type == TRANSACTIONTYPE_FAT_MONO) {
            return openForRead((FatMonoGammaTransaction) tx, lockMode);
        } else if (type == TRANSACTIONTYPE_FAT_FIXED_LENGTH) {
            return openForRead((FatFixedLengthGammaTransaction) tx, lockMode);
        } else {
            return openForRead((FatVariableLengthGammaTransaction) tx, lockMode);
        }
    }

    private static void initTranlocalForRead(final GammaTransactionConfiguration config, final GammaRefTranlocal tranlocal) {
        tranlocal.isDirty = false;
        tranlocal.mode = TRANLOCAL_READ;
        tranlocal.writeSkewCheck = config.isolationLevel == IsolationLevel.Serializable;
    }

    public final GammaRefTranlocal openForRead(final FatMonoGammaTransaction tx, int lockMode) {
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
            //we have found the tranlocal we are looking for.

            int mode = tranlocal.mode;

            if (mode == TRANLOCAL_CONSTRUCTING) {
                return tranlocal;
            }

            if (mode == TRANLOCAL_COMMUTING) {
                if (!flattenCommute(tx, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }

                return tranlocal;
            }

            if (lockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, lockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
            }

            return tranlocal;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTransactionTooSmall(2);
        }

        initTranlocalForRead(config, tranlocal);
        if (!load(tranlocal, lockMode, config.spinCount, !tx.poorMansConflictScan)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        return tranlocal;
    }

    public final GammaRefTranlocal openForRead(final FatFixedLengthGammaTransaction tx, int desiredLockMode) {
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

        desiredLockMode = config.readLockModeAsInt <= desiredLockMode ? desiredLockMode : config.readLockModeAsInt;

        if (found != null) {
            final int mode = found.mode;

            if (mode == TRANLOCAL_CONSTRUCTING) {
                return found;
            }

            if (mode == TRANLOCAL_COMMUTING) {
                if (!flattenCommute(tx, found, desiredLockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }

                return found;
            }

            if (desiredLockMode > found.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, found, desiredLockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
            }

            tx.shiftInFront(found);
            return found;
        }

        if (newNode == null) {
            throw tx.abortOnTransactionTooSmall(config.maxFixedLengthTransactionSize + 1);
        }

        tx.size++;
        tx.shiftInFront(newNode);
        initTranlocalForRead(config, newNode);

        final boolean hasReadsBeforeLoading = tx.hasReads;
        if (!hasReadsBeforeLoading) {
            tx.lastConflictCount = config.globalConflictCounter.count();
            tx.hasReads = true;
        }

        if (!load(newNode, desiredLockMode, config.spinCount, !tx.poorMansConflictScan)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        if (hasReadsBeforeLoading && !tx.isReadConsistent(newNode)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        return newNode;
    }

    public final GammaRefTranlocal openForRead(final FatVariableLengthGammaTransaction tx, int desiredLockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        //noinspection ObjectEquality
        if (config.stm != stm) {
            throw tx.abortOpenForReadOnBadStm(this);
        }

        desiredLockMode = config.readLockModeAsInt <= desiredLockMode ? desiredLockMode : config.readLockModeAsInt;

        final int identityHash = identityHashCode();
        final int indexOf = tx.indexOf(this, identityHash);

        if (indexOf > -1) {
            final GammaRefTranlocal tranlocal = tx.array[indexOf];
            final int mode = tranlocal.mode;

            if (mode == TRANLOCAL_CONSTRUCTING) {
                return tranlocal;
            }

            if (mode == TRANLOCAL_COMMUTING) {
                if (!flattenCommute(tx, tranlocal, desiredLockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }

                return tranlocal;
            }

            if (desiredLockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, desiredLockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
            }

            return tranlocal;
        }

        final GammaRefTranlocal tranlocal = tx.pool.take(this);
        initTranlocalForRead(config, tranlocal);
        tx.attach(tranlocal, identityHash);
        tx.size++;

        final boolean hasReadsBeforeLoading = tx.hasReads;
        if (!hasReadsBeforeLoading) {
            tx.hasReads = true;
            tx.lastConflictCount = config.globalConflictCounter.count();
        }

        if (!load(tranlocal, desiredLockMode, config.spinCount, !tx.poorMansConflictScan)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        if (hasReadsBeforeLoading && !tx.isReadConsistent(tranlocal)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        return tranlocal;
    }

    public final GammaRefTranlocal openForRead(final LeanFixedLengthGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus(this);
        }

        if (lockMode != LOCKMODE_NONE) {
            throw tx.abortOpenForReadOrWriteOnExplicitLocking(this);
        }

        if (tx.head.owner == this) {
            return tx.head;
        }

        //look inside the transaction if it already is opened for read or otherwise look for an empty spot to
        //place the read.
        GammaRefTranlocal found = null;
        GammaRefTranlocal newNode = null;
        GammaRefTranlocal node = tx.head;
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

        //we have found it.
        if (found != null) {
            tx.shiftInFront(found);
            return found;
        }

        //we have not found it, but there also is no spot available.
        if (newNode == null) {
            throw tx.abortOnTransactionTooSmall(tx.config.maxFixedLengthTransactionSize + 1);
        }

        final GammaTransactionConfiguration config = tx.config;

        if (config.stm != stm) {
            throw tx.abortOpenForReadOnBadStm(this);
        }

        if (type != TYPE_REF) {
            throw tx.abortOpenForReadOnNonRefTypeDetected(this);
        }

        int size = tx.size;
        if (size > config.maximumPoorMansConflictScanLength) {
            throw tx.abortOnTransactionTooBigForPoorMansConflictScan();
        }

        //load it
        newNode.mode = TRANLOCAL_READ;
        newNode.isDirty = false;
        newNode.owner = this;
        while (true) {
            //JMM: nothing can jump behind the following statement
            long readVersion;
            Object readRef;
            do {
                readRef = ref_value;
                readVersion = version;
            } while (readRef != ref_value);

            //wait for the exclusive lock to come available.
            int spinCount = 64;
            for (; ;) {
                if (!hasExclusiveLock()) {
                    break;
                }
                spinCount--;
                if (spinCount < 0) {
                    throw tx.abortOnReadWriteConflict(this);
                }
            }

            //check if the version and value we read are still the same, if they are not, we have read illegal memory,
            //so we are going to try again.
            if (readVersion == version && readRef == ref_value) {
                //at this point we are sure that the read was unlocked.
                newNode.version = readVersion;
                newNode.ref_value = readRef;
                break;
            }
        }

        tx.size = size + 1;
        //lets put it in the front it isn't the first one that is opened.
        if (tx.size > 1) {
            tx.shiftInFront(newNode);
        }

        //check if the transaction still is read consistent.
        if (tx.hasReads) {
            node = tx.head;
            do {
                //if we are at the end, we are done.
                final AbstractGammaRef owner = node.owner;

                if (owner == null) {
                    break;
                }

                if (node != newNode && (owner.hasExclusiveLock() || owner.version != node.version)) {
                    throw tx.abortOnReadWriteConflict(this);
                }

                node = node.next;
            } while (node != null);
        } else {
            tx.hasReads = true;
        }

        //we are done, the load was correct and the transaction still is read consistent.
        return newNode;
    }

    public final GammaRefTranlocal openForRead(final LeanMonoGammaTransaction tx, int lockMode) {
        if (tx.status != TX_ACTIVE) {
            throw tx.abortOpenForReadOnBadStatus(this);
        }

        if (lockMode != LOCKMODE_NONE) {
            throw tx.abortOpenForReadOrWriteOnExplicitLocking(this);
        }

        final GammaRefTranlocal tranlocal = tx.tranlocal;

        //noinspection ObjectEquality
        if (tranlocal.owner == this) {
            return tranlocal;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTransactionTooSmall(2);
        }

        final GammaTransactionConfiguration config = tx.config;

        if (config.stm != stm) {
            throw tx.abortOpenForReadOnBadStm(this);
        }

        if (type != TYPE_REF) {
            throw tx.abortOpenForReadOnNonRefTypeDetected(this);
        }

        tranlocal.mode = TRANLOCAL_READ;
        tranlocal.owner = this;
        for (; ;) {
            //do the read of the version and ref. It needs to be repeated to make sure that the version we read, belongs to the
            //value.
            Object readRef;
            long readVersion;
            do {
                readRef = ref_value;
                readVersion = version;
            } while (readRef != ref_value);

            //wait for the exclusive lock to come available.
            int spinCount = 64;
            for (; ;) {
                if (!hasExclusiveLock()) {
                    break;
                }
                spinCount--;
                if (spinCount < 0) {
                    throw tx.abortOnReadWriteConflict(this);
                }
            }

            //check if the version is still the same, if it is not, we have read illegal memory,
            //In that case we are going to try again.
            if (readVersion == version) {
                //at this point we are sure that the read was unlocked.
                tranlocal.version = readVersion;
                tranlocal.ref_value = readRef;
                break;
            }
        }

        return tranlocal;
    }

    // ============================================================================================
    // =============================== open for write =============================================
    // ============================================================================================


    public final GammaRefTranlocal openForWrite(final GammaTransaction tx, final int lockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        final int type = tx.transactionType;

        if (type == TRANSACTIONTYPE_LEAN_MONO) {
            return openForWrite((LeanMonoGammaTransaction) tx, lockMode);
        } else if (type == TRANSACTIONTYPE_LEAN_FIXED_LENGTH) {
            return openForWrite((LeanFixedLengthGammaTransaction) tx, lockMode);
        } else if (type == TRANSACTIONTYPE_FAT_MONO) {
            return openForWrite((FatMonoGammaTransaction) tx, lockMode);
        } else if (type == TRANSACTIONTYPE_FAT_FIXED_LENGTH) {
            return openForWrite((FatFixedLengthGammaTransaction) tx, lockMode);
        } else {
            return openForWrite((FatVariableLengthGammaTransaction) tx, lockMode);
        }
    }

    public final GammaRefTranlocal openForWrite(final LeanMonoGammaTransaction tx, int lockMode) {
        final GammaRefTranlocal tranlocal = openForRead(tx, lockMode);

        if (!tx.hasWrites) {
            tx.hasWrites = true;
        }

        if (tranlocal.mode == TRANLOCAL_READ) {
            tranlocal.mode = TRANLOCAL_WRITE;
        }

        return tranlocal;
    }

    public final GammaRefTranlocal openForWrite(final LeanFixedLengthGammaTransaction tx, int lockMode) {
        final GammaRefTranlocal tranlocal = openForRead(tx, lockMode);
        if (!tx.hasWrites) {
            tx.hasWrites = true;
        }

        if (tranlocal.mode == TRANLOCAL_READ) {
            tranlocal.mode = TRANLOCAL_WRITE;
        }

        return tranlocal;
    }

    private static void initTranlocalForWrite(final GammaTransactionConfiguration config, final GammaRefTranlocal tranlocal) {
        tranlocal.isDirty = !config.dirtyCheck;
        tranlocal.mode = TRANLOCAL_WRITE;
        tranlocal.writeSkewCheck = config.isolationLevel == IsolationLevel.Serializable;
    }

    public final GammaRefTranlocal openForWrite(final FatVariableLengthGammaTransaction tx, int desiredLockMode) {
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

        desiredLockMode = config.writeLockModeAsInt <= desiredLockMode ? desiredLockMode : config.writeLockModeAsInt;

        final int identityHash = identityHashCode();

        final int indexOf = tx.indexOf(this, identityHash);
        if (indexOf > -1) {
            final GammaRefTranlocal tranlocal = tx.array[indexOf];
            final int mode = tranlocal.mode;

            if (mode == TRANLOCAL_CONSTRUCTING) {
                return tranlocal;
            }

            if (mode == TRANLOCAL_COMMUTING) {
                if (!flattenCommute(tx, tranlocal, desiredLockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
                return tranlocal;
            }

            if (desiredLockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, desiredLockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
            }

            tx.hasWrites = true;
            tranlocal.setDirty(!config.dirtyCheck);
            tranlocal.mode = TRANLOCAL_WRITE;
            return tranlocal;
        }

        final GammaRefTranlocal tranlocal = tx.pool.take(this);
        initTranlocalForWrite(config, tranlocal);
        tx.attach(tranlocal, identityHash);
        tx.size++;
        tx.hasWrites = true;

        final boolean hasReadsBeforeLoading = tx.hasReads;
        if (!hasReadsBeforeLoading) {
            tx.hasReads = true;
            tx.lastConflictCount = config.globalConflictCounter.count();
        }

        if (!load(tranlocal, desiredLockMode, config.spinCount, !tx.poorMansConflictScan)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        if (hasReadsBeforeLoading && !tx.isReadConsistent(tranlocal)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        return tranlocal;
    }

    public final GammaRefTranlocal openForWrite(final FatMonoGammaTransaction tx, int desiredLockMode) {
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

        desiredLockMode = config.writeLockModeAsInt <= desiredLockMode ? desiredLockMode : config.writeLockModeAsInt;

        final GammaRefTranlocal tranlocal = tx.tranlocal;

        //noinspection ObjectEquality
        if (tranlocal.owner == this) {
            final int mode = tranlocal.mode;

            if (mode == TRANLOCAL_CONSTRUCTING) {
                return tranlocal;
            }

            if (mode == TRANLOCAL_COMMUTING) {
                if (!flattenCommute(tx, tranlocal, desiredLockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
                return tranlocal;
            }

            if (desiredLockMode > tranlocal.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, tranlocal, desiredLockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
            }

            tx.hasWrites = true;
            tranlocal.setDirty(!config.dirtyCheck);
            tranlocal.mode = TRANLOCAL_WRITE;
            return tranlocal;
        }

        if (tranlocal.owner != null) {
            throw tx.abortOnTransactionTooSmall(2);
        }

        initTranlocalForWrite(config, tranlocal);
        if (!load(tranlocal, desiredLockMode, config.spinCount, !tx.poorMansConflictScan)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        tx.hasWrites = true;
        return tranlocal;
    }

    public final GammaRefTranlocal openForWrite(final FatFixedLengthGammaTransaction tx, int lockMode) {
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
            tx.shiftInFront(found);

            final int mode = found.mode;

            if (mode == TRANLOCAL_CONSTRUCTING) {
                return found;
            }

            if (mode == TRANLOCAL_COMMUTING) {
                if (!flattenCommute(tx, found, lockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
                return found;
            }

            if (lockMode > found.getLockMode()) {
                if (!tryLockAndCheckConflict(config.spinCount, found, lockMode)) {
                    throw tx.abortOnReadWriteConflict(this);
                }
            }

            found.mode = TRANLOCAL_WRITE;
            found.setDirty(!config.dirtyCheck);
            tx.hasWrites = true;
            return found;
        }

        if (newNode == null) {
            throw tx.abortOnTransactionTooSmall(config.maxFixedLengthTransactionSize + 1);
        }

        initTranlocalForWrite(config, newNode);
        tx.hasWrites = true;
        tx.size++;
        tx.shiftInFront(newNode);

        final boolean hasReadsBeforeLoading = tx.hasReads;
        if (!hasReadsBeforeLoading) {
            tx.hasReads = true;
            tx.lastConflictCount = config.globalConflictCounter.count();
        }

        if (!load(newNode, lockMode, config.spinCount, !tx.poorMansConflictScan)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        if (hasReadsBeforeLoading && !tx.isReadConsistent(newNode)) {
            throw tx.abortOnReadWriteConflict(this);
        }

        return newNode;
    }

    // ============================================================================================
    // ================================= open for commute =========================================
    // ============================================================================================

    public final void openForCommute(final GammaTransaction tx, final Function function) {
        if (tx == null) {
            throw new NullPointerException("tx can't be null");
        }

        final int type = tx.transactionType;

        if (type == TRANSACTIONTYPE_FAT_MONO) {
            openForCommute((FatMonoGammaTransaction) tx, function);
        } else if (type == TRANSACTIONTYPE_FAT_FIXED_LENGTH) {
            openForCommute((FatFixedLengthGammaTransaction) tx, function);
        } else if (type == TRANSACTIONTYPE_FAT_VARIABLE_LENGTH) {
            openForCommute((FatVariableLengthGammaTransaction) tx, function);
        } else {
            throw tx.abortCommuteOnCommuteDetected(this);
        }
    }

    private void initTranlocalForCommute(final GammaTransactionConfiguration config, final GammaRefTranlocal tranlocal) {
        tranlocal.owner = this;
        tranlocal.mode = TRANLOCAL_COMMUTING;
        tranlocal.isDirty = !config.dirtyCheck;
        tranlocal.writeSkewCheck = false;
    }

    public final void openForCommute(final FatMonoGammaTransaction tx, final Function function) {
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
            throw tx.abortOnTransactionTooSmall(2);
        }

        tx.hasWrites = true;
        initTranlocalForCommute(config, tranlocal);
        tranlocal.addCommutingFunction(tx.pool, function);
    }

    public final void openForCommute(final FatFixedLengthGammaTransaction tx, final Function function) {
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
            throw tx.abortOnTransactionTooSmall(config.maxFixedLengthTransactionSize + 1);
        }

        tx.size++;
        tx.shiftInFront(newNode);
        tx.hasWrites = true;
        initTranlocalForCommute(config, newNode);
        newNode.addCommutingFunction(tx.pool, function);
    }

    public final void openForCommute(final FatVariableLengthGammaTransaction tx, final Function function) {
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
        initTranlocalForCommute(config, tranlocal);
        tx.hasWrites = true;
        tx.attach(tranlocal, identityHash);
        tx.size++;
        tranlocal.addCommutingFunction(tx.pool, function);
    }

    // ============================================================================================
    // ================================= try acquire =========================================
    // ============================================================================================

    @Override
    public final boolean tryAcquire(final LockMode desiredLockMode) {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return tryAcquire(tx, desiredLockMode);
    }

    public final boolean tryAcquire(final GammaTransaction tx, final LockMode desiredLockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx instanceof FatMonoGammaTransaction) {
            return tryAcquire((FatMonoGammaTransaction) tx, desiredLockMode);
        } else if (tx instanceof FatFixedLengthGammaTransaction) {
            return tryAcquire((FatFixedLengthGammaTransaction) tx, desiredLockMode);
        } else {
            return tryAcquire((FatVariableLengthGammaTransaction) tx, desiredLockMode);
        }
    }

    @Override
    public final boolean tryAcquire(final Transaction tx, final LockMode desiredLockMode) {
        return tryAcquire((GammaTransaction) tx, desiredLockMode);
    }

    public final boolean tryAcquire(final FatMonoGammaTransaction tx, final LockMode desiredLockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortTryAcquireOnBadStatus(this);
        }

        if (desiredLockMode == null) {
            throw tx.abortTryAcquireOnNullLockMode(this);
        }

        throw new TodoException();
    }

    public final boolean tryAcquire(final FatFixedLengthGammaTransaction tx, final LockMode desiredLockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortTryAcquireOnBadStatus(this);
        }

        if (desiredLockMode == null) {
            throw tx.abortTryAcquireOnNullLockMode(this);
        }

        throw new TodoException();
    }

    public final boolean tryAcquire(final FatVariableLengthGammaTransaction tx, final LockMode desiredLockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortTryAcquireOnBadStatus(this);
        }

        if (desiredLockMode == null) {
            throw tx.abortTryAcquireOnNullLockMode(this);
        }

        final GammaTransactionConfiguration config = tx.config;

        final GammaRefTranlocal tranlocal = tx.locate(this);
        if (tranlocal != null) {
            return tryLockAndCheckConflict(config.spinCount, tranlocal, desiredLockMode.asInt());
        }

        throw new TodoException();
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
            if (!hasExclusiveLock()) {
                long read = long_value;

                if (!hasExclusiveLock()) {
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
            if (!hasExclusiveLock()) {
                Object read = ref_value;
                if (!hasExclusiveLock()) {
                    return read;
                }
            }
            stm.defaultBackoffPolicy.delayedUninterruptible(attempt);
            attempt++;
        } while (attempt <= stm.spinCount);

        throw new LockedException();
    }

    public final long atomicSetLong(final long newValue, boolean returnOld) {
        final int arriveStatus = arriveAndAcquireExclusiveLockOrBackoff();

        if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
            throw new LockedException();
        }

        final long oldValue = long_value;

        if (oldValue == newValue) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockByUnregistered();
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

    public final Object atomicSetObject(final Object newValue, boolean returnOld) {
        final int arriveStatus = arriveAndAcquireExclusiveLockOrBackoff();

        if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
            throw new LockedException();
        }

        final Object oldValue = ref_value;

        if (oldValue == newValue) {
            if (arriveStatus == ARRIVE_UNREGISTERED) {
                unlockByUnregistered();
            } else {
                departAfterReadingAndUnlock();
            }

            return newValue;
        }

        ref_value = newValue;
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
        final int arriveStatus = arriveAndAcquireExclusiveLockOrBackoff();

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
                unlockByUnregistered();
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

    @Override
    public final void acquire(final LockMode desiredLockMode) {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        acquire(tx, desiredLockMode);
    }

    @Override
    public final void acquire(final Transaction tx, final LockMode desiredLockMode) {
        acquire((GammaTransaction) tx, desiredLockMode);
    }

    public final void acquire(final GammaTransaction tx, final LockMode lockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (lockMode == null) {
            throw tx.abortAcquireOnNullLockMode(this);
        }

        openForRead(tx, lockMode.asInt());
    }
}
