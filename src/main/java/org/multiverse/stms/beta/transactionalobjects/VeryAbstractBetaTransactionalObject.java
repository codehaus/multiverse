package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.NoTransactionFoundException;
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
    public final void ___markAsDurable(){
        durable = true;
    }

    @Override
    public  final boolean ___isDurable(){
        return durable;
    }
}
