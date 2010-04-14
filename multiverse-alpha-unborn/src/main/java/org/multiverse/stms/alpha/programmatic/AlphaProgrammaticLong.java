package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.Listeners;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.io.File;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.exceptions.LockNotFreeWriteConflict.createFailedToObtainCommitLocksException;
import static org.multiverse.api.exceptions.UncommittedReadConflict.createUncommittedReadConflict;

/**
 * The {@link AlphaStm} specific implementation of the ProgrammaticLong.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticLong
        extends DefaultTxObjectMixin implements ProgrammaticLong {

    private final PrimitiveClock clock;

    public static AlphaProgrammaticLong createUncommitted() {
        return new AlphaProgrammaticLong((File) null);
    }

    public AlphaProgrammaticLong(long value) {
        clock = ((AlphaStm) getGlobalStmInstance()).getClock();
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
        if (tx == null || tx.getStatus().isDead()) {
            AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) ___openUnconstructed();
            tranlocal.value = value;
            long writeVersion = clock.getVersion();
            ___storeInitial(tranlocal, writeVersion);
        } else {
            AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) tx.openForConstruction(this);
            tranlocal.value = value;
        }
    }

    public AlphaProgrammaticLong(Stm stm, long value) {
        clock = ((AlphaStm) stm).getClock();

        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) ___openUnconstructed();
        tranlocal.value = value;
        long writeVersion = clock.getVersion();
        this.___storeInitial(tranlocal, writeVersion);
    }

    public AlphaProgrammaticLong(AlphaTransaction tx, long value) {
        //todo: this is not correct.
        clock = ((AlphaStm) getGlobalStmInstance()).getClock();

        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) tx.openForConstruction(this);
        tranlocal.value = value;
    }

    private AlphaProgrammaticLong(File file) {
        clock = null;
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
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForRead(this);
        return tranlocal.value;
    }

    @Override
    public long atomicGet() {
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) ___load();

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
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForWrite(this);
        long oldValue = tranlocal.value;
        tranlocal.value = newValue;
        return oldValue;
    }

    @Override
    public long atomicSet(long newValue) {
        AlphaProgrammaticLongTranlocal committed = (AlphaProgrammaticLongTranlocal) ___load();
        //if it isn't committed.
        if (committed == null) {
            throw createUncommittedReadConflict();
        }

        //if there is no change
        if (committed.value == newValue) {
            return newValue;
        }

        //try to acquire the lock
        AlphaProgrammaticLongTranlocal newTranlocal = new AlphaProgrammaticLongTranlocal(
                this, false);
        Transaction lockOwner = (Transaction) newTranlocal;
        if (!___tryLock(lockOwner)) {
            throw createFailedToObtainCommitLocksException();
        }

        //lock was acquired successfully, we can now store the changes.
        committed = (AlphaProgrammaticLongTranlocal) ___load();

        long writeVersion = clock.tick();
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
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForWrite(this);
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
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForCommutingWrite(this);

        if (tranlocal.isCommuting()) {
            tranlocal.pendingIncrements += amount;
        } else {
            tranlocal.value += amount;
        }
    }

    @Override
    public void atomicInc(long amount) {
        if (amount == 0) {
            return;
        }

        AlphaProgrammaticLongTranlocal updateTranlocal = new AlphaProgrammaticLongTranlocal(
                this, false);
        Transaction lockOwner = (Transaction) updateTranlocal;
        if (!___tryLock(lockOwner)) {
            throw createFailedToObtainCommitLocksException();
        }

        AlphaProgrammaticLongTranlocal currentTranlocal = (AlphaProgrammaticLongTranlocal) ___load();
        if (currentTranlocal == null) {
            ___releaseLock(lockOwner);
            throw createUncommittedReadConflict();
        }

        long writeVersion = clock.tick();
        updateTranlocal.value = currentTranlocal.value + amount;
        updateTranlocal.prepareForCommit(writeVersion);
        Listeners listeners = ___storeUpdate(updateTranlocal, writeVersion, true);

        if (listeners != null) {
            listeners.openAll();
        }
    }

    @Override
    public boolean atomicCompareAndSet(long expected, long update) {
        AlphaProgrammaticLongTranlocal readonly = (AlphaProgrammaticLongTranlocal) ___load();
        if (readonly == null) {
            throw createUncommittedReadConflict();
        }

        if (readonly.value != expected) {
            return false;
        }

        if (readonly.value == update) {
            return true;
        }

        AlphaProgrammaticLongTranlocal updateTranlocal = new AlphaProgrammaticLongTranlocal(
                this, false);

        Transaction lockOwner = (Transaction) updateTranlocal;
        if (!___tryLock(lockOwner)) {
            throw createFailedToObtainCommitLocksException();
        }

        AlphaProgrammaticLongTranlocal current = (AlphaProgrammaticLongTranlocal) ___load();

        if (current.value != expected) {
            ___releaseLock(lockOwner);
            return false;
        }

        long writeVersion = clock.tick();
        updateTranlocal.value = update;
        updateTranlocal.prepareForCommit(writeVersion);
        Listeners listeners = ___storeUpdate(updateTranlocal, writeVersion, true);

        if (listeners != null) {
            listeners.openAll();
        }
        return true;
    }

    @Override
    public AlphaTranlocal ___openForCommutingOperation() {
        return new AlphaProgrammaticLongTranlocal(this, true);
    }

    @Override
    public AlphaTranlocal ___openUnconstructed() {
        return new AlphaProgrammaticLongTranlocal(this, false);
    }
}

