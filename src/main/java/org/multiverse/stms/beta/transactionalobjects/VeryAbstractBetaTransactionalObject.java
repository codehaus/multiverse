package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.TransactionRequiredException;
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
    public final long getVersion() {
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

    protected final Listeners ___removeListenersAfterWrite() {
        if (___listeners == null) {
            return null;
        }

        Listeners removedListeners;
        while (true) {
            removedListeners = ___listeners;
            if (___unsafe.compareAndSwapObject(this, listenersOffset, removedListeners, null)) {
                return removedListeners;
            }
        }
    }

    @Override
    public final int ___registerChangeListener(
            final Latch latch,
            final Tranlocal tranlocal,
            final BetaObjectPool pool,
            final long listenerEra) {

        if (tranlocal.isCommuting() || tranlocal.isConstructing()) {
            return REGISTRATION_NONE;
        }

        final long version = tranlocal.version;

        if (version != ___version) {
            //if it currently already contains a different version, we are done.
            latch.open(listenerEra);
            return REGISTRATION_NOT_NEEDED;
        }

        //we are going to register the listener since the current value still matches with is active.
        //But it could be that the registration completes after the write has happened.

        Listeners update = pool.takeListeners();
        if (update == null) {
            update = new Listeners();
        }
        update.threadName = Thread.currentThread().getName();
        update.listener = latch;
        update.listenerEra = listenerEra;

        //we need to do this in a loop because other register thread could be contending for the same
        //listeners field.
        while (true) {
            if (version != ___version) {
                //if it currently already contains a different version, we are done.
                latch.open(listenerEra);
                return REGISTRATION_NOT_NEEDED;
            }

            //the listeners object is mutable, but as long as it isn't yet registered, this calling
            //thread has full ownership of it.
            final Listeners current = ___listeners;
            update.next = current;

            //lets try to register our listeners.
            final boolean registered = ___unsafe.compareAndSwapObject(this, listenersOffset, current, update);
            if (!registered) {
                //so we are contending with another register thread, so lets try it again. Since the compareAndSwap
                //didn't succeed, we know that the current thread still has exclusive ownership on the Listeners object
                //so we can try to register it again, but now with the newly found listeners
                continue;
            }

            //the registration was a success. We need to make sure that the ___version hasn't changed.
            //JMM: the volatile read of ___version can't jump in front of the unsafe.compareAndSwap.
            if (version == ___version) {
                //we are lucky, the registration was done successfully and we managed to cas the listener
                //before the update (since the update we are interested in, hasn't happened yet). This means that
                //the updating thread is now responsible for notifying the listeners. Retrieval of the most recently
                //published listener, always happens after the version is updated
                return REGISTRATION_DONE;
            }

            //the version has changed, so an interesting write has happened. No registration is needed.
            //JMM: the unsafe.compareAndSwap can't jump over the volatile read this.___version.
            //the update has taken place, we need to check if our listeners still is in place.
            //if it is, it should be removed and the listeners notified. If the listeners already has changed,
            //it is the task for the other to do the listener cleanup and notify them
            while (true) {
                update = ___listeners;
                final boolean removed = ___unsafe.compareAndSwapObject(this, listenersOffset, update, null);

                if (!removed) {
                    continue;
                }

                if (update != null) {
                    //we have complete ownership of the listeners that are removed, so lets open them.
                    update.openAll(pool);
                }
                return REGISTRATION_NOT_NEEDED;
            }
        }
    }

    @Override
    public final boolean ___tryLockAndCheckConflict(
            final BetaTransaction newLockOwner,
            final int spinCount,
            final Tranlocal tranlocal,
            final boolean commitLock) {

        final int currentLockMode = tranlocal.getLockMode();

        if (currentLockMode != LOCKMODE_NONE) {
            if (commitLock && currentLockMode == LOCKMODE_UPDATE) {
                tranlocal.setLockMode(LOCKMODE_COMMIT);
                ___upgradeToCommitLock();
            }
            return true;
        }

        final long expectedVersion = tranlocal.version;

        //if the version already is different, we are done since we know that there is a conflict.
        if (___version != expectedVersion) {
            return false;
        }

        if (!tranlocal.hasDepartObligation()) {
            //we need to arrive as well because the the tranlocal was readbiased, and no real arrive was done.
            final int arriveStatus = ___tryLockAndArrive(spinCount, commitLock);

            if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                return false;
            }

            if (arriveStatus == ARRIVE_NORMAL) {
                tranlocal.setDepartObligation(true);
            }
        } else if (!___tryLockAfterNormalArrive(spinCount, commitLock)) {
            return false;
        }

        //the lock was acquired successfully.
        ___lockOwner = newLockOwner;
        tranlocal.setLockMode(commitLock ? LOCKMODE_COMMIT : LOCKMODE_UPDATE);
        return expectedVersion == ___version;
    }

    @Override
    public final boolean ___hasReadConflict(final Tranlocal tranlocal) {
        if (tranlocal.getLockMode() != LOCKMODE_NONE) {
            return false;
        }

        if (___hasCommitLock()) {
            return true;
        }

        return tranlocal.version != ___version;
    }

    @Override
    public final boolean atomicIsFree() {
        return !___hasLock();
    }

    @Override
    public final boolean atomicIsPrivatized() {
        return ___hasCommitLock();
    }

    @Override
    public final boolean isPrivatizedBySelf() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException("No transaction is found for the isPrivatizedBySelf operation");

        }

        return isPrivatizedBySelf(tx);
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

        if (tx == null) {
            throw new TransactionRequiredException("No transaction is found for the isPrivatizedByOther operation");
        }

        return isPrivatizedByOther(tx);
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
    public final boolean atomicIsEnsured() {
        return ___hasUpdateLock();
    }

    @Override
    public final boolean isEnsuredBySelf() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException("No transaction is found for the isEnsuredBySelf operation");
        }

        return isEnsuredBySelf(tx);
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

        if (tx == null) {
            throw new TransactionRequiredException("No transaction is found for the isEnsuredByOther operation");
        }

        return isEnsuredByOther(tx);
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
