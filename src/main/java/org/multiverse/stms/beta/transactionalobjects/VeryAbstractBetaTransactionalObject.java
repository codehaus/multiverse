package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.orec.FastOrec;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.UUID;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public abstract class VeryAbstractBetaTransactionalObject
        extends FastOrec
        implements BetaTransactionalObject {

    protected final static long listenersOffset;

    static {
        try {
            listenersOffset = ___unsafe.objectFieldOffset(
                    VeryAbstractBetaTransactionalObject.class.getDeclaredField("___listeners"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    protected BetaTransaction ___lockOwner;

    protected volatile Listeners ___listeners;

    protected volatile long ___version;

    //This field has a controlled JMM problem (just like the hashcode of String).
    protected int ___identityHashCode;
    protected final BetaStm ___stm;

    public VeryAbstractBetaTransactionalObject(BetaStm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.___stm = stm;
    }

    @Override
    public long getVersion() {
        return ___version;
    }

    @Override
    public final BetaStm getStm() {
        return ___stm;
    }

    @Override
    public final BetaTransaction ___getLockOwner() {
        return ___lockOwner;
    }

    @Override
    public final Orec ___getOrec() {
        return this;
    }

    @Override
    public final int ___registerChangeListener(
            final Latch latch,
            final Tranlocal tranlocal,
            final BetaObjectPool pool,
            final long listenerEra) {

        if (tranlocal.isCommuting || tranlocal.isConstructing) {
            return REGISTRATION_NONE;
        }

        if (tranlocal.version != ___version) {
            //if it currently already contains a different version, we are done.
            latch.open(listenerEra);
            return REGISTRATION_NOT_NEEDED;
        }

        //we are going to register the listener since the current value still matches with is active.
        //But it could be that the registration completes after the write has happened.

        Listeners newListeners = pool.takeListeners();
        if (newListeners == null) {
            newListeners = new Listeners();
        }
        newListeners.listener = latch;
        newListeners.listenerEra = listenerEra;

        //we need to do this in a loop because other register thread could be contending for the same
        //listeners field.
        while (true) {
            //the listeners object is mutable, but as long as it isn't yet registered, this calling
            //thread has full ownership of it.
            final Listeners oldListeners = ___listeners;

            newListeners.next = oldListeners;

            //lets try to register our listeners.
            if (!___unsafe.compareAndSwapObject(this, listenersOffset, oldListeners, newListeners)) {
                //so we are contending with another register thread, so lets try it again. Since the compareAndSwap
                //didn't succeed, we know that the current thread still has exclusive ownership on the Listeners object.
                continue;
            }

            //the registration was a success. We need to make sure that the active hasn't changed.
            //JMM: the volatile read can't jump in front of the unsafe.compareAndSwap.
            if (tranlocal.version == ___version) {
                //we are lucky, the registration was done successfully and we managed to cas the listener
                //before the update (since the update hasn't happened yet). This means that the updating thread
                //is now responsible for notifying the listeners.
                return REGISTRATION_DONE;
            }

            //JMM: the unsafe.compareAndSwap can't jump over the volatile read this.___version.
            //the update has taken place, we need to check if our listeners still is in place.
            //if it is, it should be removed and the listeners notified. If the listeners already has changed,
            //it is the task for the other to do the listener cleanup and notify them
            if (___unsafe.compareAndSwapObject(this, listenersOffset, newListeners, null)) {
                newListeners.openAll(pool);
            } else {
                latch.open(listenerEra);
            }

            return REGISTRATION_NOT_NEEDED;
        }
    }

    @Override
    public final boolean ___tryLockAndCheckConflict(
            final BetaTransaction newLockOwner,
            final int spinCount,
            final Tranlocal tranlocal,
            final boolean commitLock) {

        final int lockMode = tranlocal.lockMode;

        if (lockMode != LOCKMODE_NONE) {
            if (commitLock && lockMode == LOCKMODE_UPDATE) {
                tranlocal.lockMode = LOCKMODE_COMMIT;
                ___upgradeToCommitLock();
            }
            return true;
        }

        final long expectedVersion = tranlocal.version;

        //if the version already is different, we are done since we know that there is a conflict.
        if (___version != expectedVersion) {
            return false;
        }

        if (!tranlocal.hasDepartObligation) {
            //we need to arrive as well because the the tranlocal was readbiased, and no real arrive was done.
            final int arriveStatus = ___tryLockAndArrive(spinCount, commitLock);

            if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                return false;
            }

            if (arriveStatus == ARRIVE_NORMAL) {
                tranlocal.hasDepartObligation = true;
            }
        } else if (!___tryLockAfterNormalArrive(spinCount, commitLock)) {
            return false;
        }

        //the lock was acquired successfully.
        ___lockOwner = newLockOwner;
        tranlocal.lockMode = commitLock ? LOCKMODE_COMMIT : LOCKMODE_UPDATE;
        return expectedVersion == ___version;
    }

    protected final Listeners ___removeListenersAfterWrite() {
        if (___listeners == null) {
            return null;
        }

        //at this point it could have happened that the listener has changed.. it could also
        Listeners result;
        while (true) {
            result = ___listeners;
            if (___unsafe.compareAndSwapObject(this, listenersOffset, result, null)) {
                return result;
            }
        }
    }

    @Override
    public final boolean ___hasReadConflict(final Tranlocal tranlocal) {
        if (tranlocal.lockMode != LOCKMODE_NONE) {
            return false;
        }

        if (___hasCommitLock()) {
            return true;
        }

        return tranlocal.version != ___version;
    }

    @Override
    public final boolean isFree() {
        return !___hasLock();
    }

    @Override
    public final boolean isPrivatized() {
        return ___hasCommitLock();
    }

    @Override
    public final boolean isPrivatizedBySelf() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx != null && tx.isAlive()) {
            return isPrivatizedBySelf(tx);
        }

        throw new NoTransactionFoundException("No transaction is found for the isPrivatizedBySelf operation");
    }

    @Override
    public final boolean isPrivatizedBySelf(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (!___hasCommitLock()) {
            return false;
        }

        return ___lockOwner == tx;
    }

    @Override
    public final boolean isPrivatizedByOther() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx != null && tx.isAlive()) {
            return isPrivatizedByOther(tx);
        }

        throw new NoTransactionFoundException("No transaction is found for the isPrivatizedByOther operation");
    }

    @Override
    public final boolean isPrivatizedByOther(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (!___hasCommitLock()) {
            return false;
        }

        return ___lockOwner != tx;
    }

    @Override
    public final boolean isEnsured() {
        return ___hasUpdateLock();
    }

    @Override
    public final boolean isEnsuredBySelf() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx != null && tx.isAlive()) {
            return isEnsuredBySelf(tx);
        }

        throw new NoTransactionFoundException("No transaction is found for the isEnsuredBySelf operation");
    }

    @Override
    public final boolean isEnsuredBySelf(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (!___hasUpdateLock()) {
            return false;
        }

        return ___lockOwner == tx;
    }

    @Override
    public final boolean isEnsuredByOther() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx != null && tx.isAlive()) {
            return isEnsuredByOther(tx);
        }

        throw new NoTransactionFoundException("No transaction is found for the isEnsuredByOther operation");
    }

    @Override
    public final boolean isEnsuredByOther(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (!___hasUpdateLock()) {
            return false;
        }

        return ___lockOwner != tx;
    }

    protected final int ___arriveAndLockOrBackoff() {
        for (int k = 0; k <= ___stm.defaultMaxRetries; k++) {
            final int arriveStatus = ___tryLockAndArrive(___stm.spinCount, true);
            if (arriveStatus != ARRIVE_LOCK_NOT_FREE) {
                return arriveStatus;
            }

            ___stm.defaultBackoffPolicy.delayedUninterruptible(k + 1);
        }

        return ARRIVE_LOCK_NOT_FREE;
    }

    //a controlled jmm problem here since identityHashCode is not synchronized/volatile/final.
    //this is the same as with the hashcode and String.
    @Override
    public final int ___identityHashCode() {
        int tmp = ___identityHashCode;
        if (tmp != 0) {
            return tmp;
        }

        tmp = System.identityHashCode(this);
        ___identityHashCode = tmp;
        return tmp;
    }


    private String storageId = UUID.randomUUID().toString();

    private volatile boolean durable = false;

    @Override
    public final String ___getStorageId() {
        return storageId;
    }

    @Override
    public final void ___setStorageId(final String id) {
        this.storageId = id;
    }

    @Override
    public final void ___markAsDurable() {
        durable = true;
    }

    @Override
    public final boolean ___isDurable() {
        return durable;
    }
}
