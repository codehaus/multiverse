package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.Listeners;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.api.programmatic.ProgrammaticLongRef;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.mixins.BasicMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.io.File;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.exceptions.UncommittedReadConflict.createUncommittedReadConflict;

/**
 * The {@link AlphaStm} specific implementation of the ProgrammaticLongRef.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticLongRef extends BasicMixin implements ProgrammaticLongRef {

    private final AlphaStm stm;

    //should only be used for testing purposes.

    public static AlphaProgrammaticLongRef createUncommitted(AlphaStm stm) {
        return new AlphaProgrammaticLongRef(stm, (File) null);
    }

    public AlphaProgrammaticLongRef(long value) {
        stm = (AlphaStm) getGlobalStmInstance();

        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
        if (tx == null || tx.getStatus().isDead()) {
            AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) ___openUnconstructed();
            tranlocal.value = value;
            long writeVersion = stm.getVersion();
            ___storeInitial(tranlocal, writeVersion);
        } else {
            AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) tx.openForConstruction(this);
            tranlocal.value = value;
        }
    }

    public AlphaProgrammaticLongRef(AlphaStm stm, long value) {
        if (stm == null) {
            throw new NullPointerException();
        }

        this.stm = stm;

        AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) ___openUnconstructed();
        tranlocal.value = value;
        long writeVersion = stm.getVersion();
        this.___storeInitial(tranlocal, writeVersion);
    }

    public AlphaProgrammaticLongRef(AlphaTransaction tx, long value) {
        if (tx == null) {
            throw new NullPointerException();
        }

        this.stm = (AlphaStm) tx.getTransactionFactory().getStm();

        AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) tx.openForConstruction(this);
        tranlocal.value = value;
    }

    //has a strange argument type to prevent name clashes.

    private AlphaProgrammaticLongRef(AlphaStm stm, File file) {
        this.stm = stm;
    }

    @Override
    public long get() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return atomicGet();
        }

        return get(tx);
    }

    @Override
    public long get(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) alphaTx.openForRead(this);
        return tranlocal.value;
    }

    @Override
    public long atomicGet() {
        AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) ___load();

        if (tranlocal == null) {
            return 0;
        }

        return tranlocal.value;
    }

    // =========================== set =========================

    public long set(long newValue) {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return atomicSet(newValue);
        }

        return set(tx, newValue);
    }

    @Override
    public long set(Transaction tx, long newValue) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) alphaTx.openForWrite(this);
        long oldValue = tranlocal.value;
        tranlocal.value = newValue;
        return oldValue;
    }

    @Override
    public long atomicSet(long newValue) {
        AlphaProgrammaticLongRefTranlocal committed = (AlphaProgrammaticLongRefTranlocal) ___load();
        //if it isn't committed.
        if (committed == null) {
            throw createUncommittedReadConflict();
        }

        //if there is no change
        if (committed.value == newValue) {
            return newValue;
        }

        //try to acquire the lock
        AlphaProgrammaticLongRefTranlocal newTranlocal = new AlphaProgrammaticLongRefTranlocal(
                this, false);
        Transaction lockOwner = newTranlocal;
        lockOwner.setAttempt(1);
        //if we couldn't acquire the lock, we are done.
        lock(lockOwner);

        //lock was acquired successfully, we can now store the changes.
        committed = (AlphaProgrammaticLongRefTranlocal) ___load();

        long writeVersion = stm.getClock().tick();
        newTranlocal.value = newValue;

        newTranlocal.prepareForCommit(writeVersion);
        Listeners listeners = ___storeUpdate(newTranlocal, writeVersion, true);

        if (listeners != null) {
            listeners.openAll();
        }
        return committed.value;
    }

    // ============================= inc ============================

    public void inc(long amount) {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            atomicInc(amount);
            return;
        }

        inc(tx, amount);
    }

    @Override
    public void inc(Transaction tx, long amount) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) alphaTx.openForWrite(this);
        tranlocal.value += amount;
    }

    @Override
    public void commutingInc(long amount) {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            atomicInc(amount);
            return;
        }

        commutingInc(tx, amount);
    }

    @Override
    public void commutingInc(Transaction tx, long amount) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (amount == 0) {
            return;
        }

        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) alphaTx.openForCommutingWrite(this);

        if (tranlocal.isCommuting()) {
            tranlocal.commutingIncrements += amount;
        } else {
            tranlocal.value += amount;
        }
    }

    @Override
    public void atomicInc(long amount) {
        if (amount == 0) {
            return;
        }

        AlphaProgrammaticLongRefTranlocal updateTranlocal = new AlphaProgrammaticLongRefTranlocal(
                this, false);
        Transaction lockOwner = (Transaction) updateTranlocal;
        lockOwner.setAttempt(1);

        lock(lockOwner);

        AlphaProgrammaticLongRefTranlocal currentTranlocal = (AlphaProgrammaticLongRefTranlocal) ___load();
        if (currentTranlocal == null) {
            ___releaseLock(lockOwner);
            throw createUncommittedReadConflict();
        }

        long writeVersion = stm.getClock().tick();
        updateTranlocal.value = currentTranlocal.value + amount;
        updateTranlocal.prepareForCommit(writeVersion);
        Listeners listeners = ___storeUpdate(updateTranlocal, writeVersion, true);

        if (listeners != null) {
            listeners.openAll();
        }
    }

    private void lock(Transaction lockOwner) {
        //if we couldn't acquire the lock, we are done.
        for (int attempt = 1; attempt <= stm.getMaxRetries(); attempt++) {
            lockOwner.setAttempt(attempt);

            if (attempt == stm.getMaxRetries()) {
                throw new TooManyRetriesException();
            } else if (___tryLock(lockOwner)) {
                return;
            } else {
                stm.getBackoffPolicy().delayedUninterruptible(lockOwner);
            }
        }
    }

    @Override
    public boolean atomicCompareAndSet(long expected, long update) {
        AlphaProgrammaticLongRefTranlocal readonly = (AlphaProgrammaticLongRefTranlocal) ___load();
        if (readonly == null) {
            throw createUncommittedReadConflict();
        }

        if (readonly.value != expected) {
            return false;
        }

        if (readonly.value == update) {
            return true;
        }

        AlphaProgrammaticLongRefTranlocal updateTranlocal = new AlphaProgrammaticLongRefTranlocal(
                this, false);

        Transaction lockOwner = (Transaction) updateTranlocal;
        lockOwner.setAttempt(1);

        if (!___tryLock(lockOwner)) {
            return false;
        }

        AlphaProgrammaticLongRefTranlocal current = (AlphaProgrammaticLongRefTranlocal) ___load();

        if (current.value != expected) {
            ___releaseLock(lockOwner);
            return false;
        }

        long writeVersion = stm.getClock().tick();
        updateTranlocal.value = update;
        updateTranlocal.prepareForCommit(writeVersion);
        Listeners listeners = ___storeUpdate(updateTranlocal, writeVersion, true);

        if (listeners != null) {
            listeners.openAll();
        }
        return true;
    }

    // ======================== mod ============================================

    @Override
    public void changeMod() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            atomicChangeMod();
            return;
        }

        //return changeMod(tx);
    }

    @Override
    public void changeMod(Transaction tx) {

    }

    @Override
    public void atomicChangeMod() {

    }

    @Override
    public AlphaTranlocal ___openForCommutingOperation() {
        return new AlphaProgrammaticLongRefTranlocal(this, true);
    }

    @Override
    public AlphaTranlocal ___openUnconstructed() {
        return new AlphaProgrammaticLongRefTranlocal(this, false);
    }
}

