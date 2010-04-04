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
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.utils.TodoException;

import java.io.File;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.exceptions.CommitLockNotFreeWriteConflict.createFailedToObtainCommitLocksException;
import static org.multiverse.api.exceptions.UncommittedReadConflict.createUncommittedReadConflict;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong extends DefaultTxObjectMixin implements ProgrammaticLong {

    private static final PrimitiveClock clock = ((AlphaStm) getGlobalStmInstance()).getClock();

    public static AlphaProgrammaticLong createUncommitted() {
        return new AlphaProgrammaticLong((File) null);
    }

    public AlphaProgrammaticLong(final long value) {
        //template can be removed.
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction tx) throws Exception {
                AlphaTransaction alphaTx = (AlphaTransaction) tx;
                AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForConstruction(AlphaProgrammaticLong.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public AlphaProgrammaticLong(Stm stm, final long value) {
        //template can be removed.
        new TransactionTemplate(stm) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                AlphaTransaction alphaTx = (AlphaTransaction) tx;
                AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForConstruction(AlphaProgrammaticLong.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public AlphaProgrammaticLong(AlphaTransaction tx, long value) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) tx.openForConstruction(this);
        tranlocal.value = value;
    }

    private AlphaProgrammaticLong(File file) {
    }

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
        AlphaProgrammaticLongTranlocal newTranlocal = new AlphaProgrammaticLongTranlocal(
                this, false);
        Transaction lockOwner = (Transaction) newTranlocal;
        if (!___tryLock(lockOwner)) {
            throw createFailedToObtainCommitLocksException();
        }

        AlphaProgrammaticLongTranlocal current = (AlphaProgrammaticLongTranlocal) ___load();
        if (current == null) {
            ___releaseLock(lockOwner);
            throw createUncommittedReadConflict();
        }

        long writeVersion = clock.tick();
        newTranlocal.value = newValue;
        newTranlocal.prepareForCommit(writeVersion);
        Listeners listeners = ___storeUpdate(newTranlocal, writeVersion, true);

        if (listeners != null) {
            listeners.openAll();
        }
        return current.value;
    }

    // ============================= inc ============================

    public long inc(long amount) {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return atomicInc(amount);
        }

        return inc(tx, amount);
    }

    @Override
    public long inc(Transaction tx, long amount) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForWrite(this);
        tranlocal.value += amount;
        return tranlocal.value;
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
    public long atomicInc(long amount) {
        if (amount == 0) {
            return atomicGet();
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
        updateTranlocal.value = currentTranlocal.value += amount;
        updateTranlocal.prepareForCommit(writeVersion);
        Listeners listeners = ___storeUpdate(updateTranlocal, writeVersion, true);

        if (listeners != null) {
            listeners.openAll();
        }

        return currentTranlocal.value;
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
    public long atomicGetVersion() {
        throw new TodoException();
    }

    @Override
    public long getVersion() {
        throw new TodoException();
    }

    @Override
    public long getVersion(Transaction tx) {
        throw new TodoException();
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

