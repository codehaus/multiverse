package org.multiverse.stms.gamma;

import org.multiverse.api.exceptions.LoadLockedException;

import java.util.concurrent.atomic.AtomicReference;

public class GammaTransactionalObject {

    public final AtomicReference<GammaTransaction> ___lockOwnerRef = new AtomicReference<GammaTransaction>();
    public final AtomicReference<GammaTranlocal> ___currentRef = new AtomicReference<GammaTranlocal>();

    public GammaTransactionalObject(GammaTransaction transaction) {
        transaction.load(this);
    }

    public GammaTransactionalObject() {
        GammaTransaction transaction = new GammaTransaction();
        transaction.load(this);
        transaction.commit();
    }

    public void inc() {
        GammaTransaction transaction = new GammaTransaction();
        inc(transaction);
        transaction.commit();
    }

    public void inc(GammaTransaction transaction) {
        transaction.load(this).value++;
    }

    public void set(GammaTransaction transaction, int value) {
        transaction.load(this).value = value;
    }

    public int get() {
        GammaTransaction transaction = new GammaTransaction();
        int result = transaction.load(this).value;
        transaction.commit();
        return result;
    }

    public int get(GammaTransaction transaction) {
        return transaction.load(this).value;
    }

    //================================= stm specific ====================================

    /**
     * Returns the most recent written tranlocal.
     *
     * @return the most recent written tranlocal.
     * @throws org.multiverse.api.exceptions.LoadLockedException
     *          if the DeltaAtomicObject was locked.
     */
    public GammaTranlocal ___load() {
        if (___isLocked()) {
            throw new LoadLockedException();
        }

        GammaTranlocal tranlocal = ___currentRef.get();
        if (___isLocked()) {
            throw new LoadLockedException();
        }
        return tranlocal;
    }

    public GammaTranlocal ___loadRaw() {
        return ___currentRef.get();
    }


    /**
     * Locks the DeltaAtomicObject. The lock call is not reentrant, so be careful.
     *
     * @param lockOwner the transaction that wants to own the lock.
     * @return true if the lock was acquired successfully, false otherwise.
     */
    public boolean ___lock(GammaTransaction lockOwner) {
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
    public boolean ___unlock(GammaTransaction expectedLockOwner) {
        return ___lockOwnerRef.compareAndSet(expectedLockOwner, null);
    }

    public void ___store(GammaTranlocal tranlocal, long writeVersion) {
        tranlocal.___origin = null;
        tranlocal.___version = writeVersion;
        //it is important that the modifications done on the tranlocal, are executed before the
        //write to the currentRef is done, else other transactions could see inconsistent state.
        ___currentRef.set(tranlocal);
    }

    public GammaTranlocal ___createInitialTranlocal() {
        return new GammaTranlocal();
    }

}
