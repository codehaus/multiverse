package org.multiverse.stms.delta;

import org.multiverse.api.exceptions.LoadLockedException;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Is there a relation between the newestSpottenVersion and currentRef.___version? Should the newestSpottedVersion
 * always be higher or equal to currentRef.___version?
 * <p/>
 * Once the tranlocal has been stored in the currentRef, the content of the tranlocal should not change.
 *
 * @author Peter Veentjer
 */
public class DeltaAtomicObject {

    public final AtomicReference<DeltaTransaction> ___lockOwnerRef = new AtomicReference<DeltaTransaction>();
    public final AtomicReference<DeltaTranlocal> ___currentRef = new AtomicReference<DeltaTranlocal>();
    public final AtomicLong ___highestTransactionVersion = new AtomicLong();

    public DeltaAtomicObject(DeltaTransaction transaction) {
        transaction.loadUpdatable(this);
    }

    public DeltaAtomicObject() {
        DeltaTransaction transaction = new DeltaTransaction();
        transaction.loadUpdatable(this);
        transaction.commit();
    }

    public void inc() {
        DeltaTransaction transaction = new DeltaTransaction();
        inc(transaction);
        transaction.commit();
    }

    public void inc(DeltaTransaction transaction) {
        transaction.loadUpdatable(this).value++;
    }

    public void set(DeltaTransaction transaction, int value) {
        transaction.loadUpdatable(this).value = value;
    }

    public int get() {
        DeltaTransaction transaction = new DeltaTransaction();
        int result = transaction.loadReadonly(this).value;
        transaction.commit();
        return result;
    }

    public int get(DeltaTransaction transaction) {
        return transaction.loadReadonly(this).value;
    }

    //================================= stm specific ====================================

    /**
     * Returns the most recent written tranlocal.
     *
     * @return the most recent written tranlocal.
     * @throws LoadLockedException if the DeltaAtomicObject was locked.
     */
    public DeltaTranlocal ___load() {
        if (___isLocked()) {
            throw new LoadLockedException();
        }
        DeltaTranlocal tranlocal = ___currentRef.get();
        if (___isLocked()) {
            throw new LoadLockedException();
        }
        return tranlocal;
    }

    public DeltaTranlocal ___loadRaw() {
        return ___currentRef.get();
    }


    /**
     * Locks the DeltaAtomicObject. The lock call is not reentrant, so be careful.
     *
     * @param lockOwner the transaction that wants to own the lock.
     * @return true if the lock was acquired successfully, false otherwise.
     */
    public boolean ___lock(DeltaTransaction lockOwner) {
        return ___lockOwnerRef.compareAndSet(null, lockOwner);
    }

    /**
     * Checks if this DeltaAtomicObject is locked.
     *
     * @return true if the lock is locked, false otherwise
     */
    public boolean ___isLocked() {
        return ___lockOwnerRef.get() != null;
    }

    /**
     * Unlocks the DeltaAtomicObject. Lock is only unlocked if the expectedLockOwner is the lockOwner.
     *
     * @param expectedLockOwner the expected Transaction that owns the lock.
     * @return true if the lock is released successfully, false otherwise.
     */
    public boolean ___unlock(DeltaTransaction expectedLockOwner) {
        return ___lockOwnerRef.compareAndSet(expectedLockOwner, null);
    }

    public void ___store(DeltaTranlocal tranlocal, long writeVersion) {
        tranlocal.___origin = null;
        tranlocal.___version = writeVersion;
        //it is important that the modifications done on the tranlocal, are executed before the
        //write to the currentRef is done, else other transactions could see inconsistent state.
        ___currentRef.set(tranlocal);
    }

    public DeltaTranlocal ___createInitialTranlocal() {
        return new DeltaTranlocal();
    }

    public long ___getHighestTransactionVersion() {
        return ___highestTransactionVersion.get();
    }

    public void ___setHighestTransactionVersion(long version) {
        while (true) {
            long highest = ___highestTransactionVersion.get();

            if (highest >= version) {
                return;
            }

            if (___highestTransactionVersion.compareAndSet(highest, version)) {
                return;
            }
        }
    }
}
