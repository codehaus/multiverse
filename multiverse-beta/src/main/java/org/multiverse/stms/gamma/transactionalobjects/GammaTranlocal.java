package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class GammaTranlocal implements GammaConstants {

    public long long_value;
    public long long_oldValue;

    public long version;
    private int lockMode;
    public GammaObject owner;
    public int mode;
    private boolean hasDepartObligation;
    public GammaTranlocal next;
    public GammaTranlocal previous;
    public boolean isDirty = false;



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


    public boolean prepare(GammaTransactionConfiguration config) {
        if (!isWrite()) {
            return true;
        }

        if (!isDirty()) {
            boolean isDirty = long_value != long_oldValue;

            if (!isDirty) {
                return true;
            }

            setDirty(true);
        }

        return owner.tryCommitLockAndCheckConflict(config.spinCount, this);
    }


    public int getMode() {
        return mode;
    }
}
