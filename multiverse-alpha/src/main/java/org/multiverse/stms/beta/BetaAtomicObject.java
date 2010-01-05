package org.multiverse.stms.beta;

import org.multiverse.api.exceptions.LoadTooOldVersionException;

/**
 * @author Peter Veentjer
 */
public abstract class BetaAtomicObject {

    private BetaTransaction lockOwner;
    private BetaTranlocal tranlocal;

    public synchronized BetaTranlocal load() {
        return tranlocal;
    }

    public synchronized BetaTranlocal loadReadonly(long version) {
        if (tranlocal == null) {
            return null;
        }

        if (tranlocal.___version > version) {
            throw new LoadTooOldVersionException();
        }

        return tranlocal;
    }

    public synchronized void write(BetaTranlocal tranlocal) {
        if (tranlocal == null) {
            throw new NullPointerException();
        }
        this.tranlocal = tranlocal;
    }

    public synchronized boolean lock(BetaTransaction owner) {
        if (lockOwner == null) {
            lockOwner = owner;
            return true;
        } else {
            return lockOwner == owner;
        }
    }

    public synchronized void unlock(BetaTransaction expectedOwner) {
        if (lockOwner == null) {
            return;
        }

        if (lockOwner == expectedOwner) {
            lockOwner = null;
        }
    }

    public abstract BetaTranlocal createInitialTranlocal();
}
