package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.functions.Function;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;

public final class GammaRefTranlocal<E> implements GammaConstants {

    public long long_value;
    public long long_oldValue;
    public E ref_value;
    public E ref_oldValue;

    public long version;
    private int lockMode;
    public AbstractGammaRef owner;
    public int mode;
    private boolean hasDepartObligation;
    public GammaRefTranlocal next;
    public GammaRefTranlocal previous;
    public boolean isDirty = false;
    public CallableNode headCallable;
    public boolean writeSkewCheck = false;

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public int getLockMode() {
        return lockMode;
    }

    public void setLockMode(int lockMode) {
        this.lockMode = lockMode;
    }

    public boolean hasDepartObligation() {
        return hasDepartObligation;
    }

    public void setDepartObligation(boolean b) {
        this.hasDepartObligation = b;
    }

    public boolean isCommuting() {
        return mode == TRANLOCAL_COMMUTING;
    }

    public boolean isConstructing() {
        return mode == TRANLOCAL_CONSTRUCTING;
    }

    public boolean isRead() {
        return mode == TRANLOCAL_READ;
    }

    public boolean isWrite() {
        return mode == TRANLOCAL_WRITE;
    }

    public void addCommutingFunction(GammaObjectPool pool, Function function) {
        final CallableNode newHead = pool.takeCallableNode();
        newHead.function = function;
        newHead.next = headCallable;
        headCallable = newHead;
    }

    public int getMode() {
        return mode;
    }

    public boolean isConflictCheckNeeded() {
        return writeSkewCheck;
    }
}
