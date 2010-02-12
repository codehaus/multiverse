package org.multiverse.stms.beta;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LoadLockedException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;

public abstract class AbstractBetaObject implements BetaObject {

    public Transaction lockOwner;

    @Override
    public BetaTranlocal ___load(long readVersion) {
        if (lockOwner != null) {
            throw new LoadLockedException();
        }

        BetaTranlocal current = ___load();
        if (current == null) {
            return null;
        }

        if (current.___getWriteVersion() > readVersion) {
            throw new LoadTooOldVersionException();
        }

        return current;
    }
}
