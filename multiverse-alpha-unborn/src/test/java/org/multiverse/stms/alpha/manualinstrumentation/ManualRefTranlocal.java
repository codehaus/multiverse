package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;

public class ManualRefTranlocal extends AlphaTranlocal {

    public int value;

    public ManualRefTranlocal(ManualRefTranlocal origin) {
        this.___origin = origin;
        this.___transactionalObject = origin.___transactionalObject;
        this.value = origin.value;
    }

    public ManualRefTranlocal(ManualRef txObject) {
        this.___transactionalObject = txObject;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new ManualRefTranlocal(this);
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        ManualRefTranlocal origin = (ManualRefTranlocal) ___origin;
        if (origin.value != value) {
            return true;
        }

        return false;
    }

    @Override
    public ManualRefTranlocalSnapshot takeSnapshot() {
        return new ManualRefTranlocalSnapshot(this);
    }
}

class ManualRefTranlocalSnapshot extends AlphaTranlocalSnapshot {

    final ManualRefTranlocal ___tranlocal;
    final int value;

    public ManualRefTranlocalSnapshot(ManualRefTranlocal tranlocal) {
        this.___tranlocal = tranlocal;
        this.value = tranlocal.value;
    }

    @Override
    public AlphaTranlocal getTranlocal() {
        return ___tranlocal;
    }

    @Override
    public void restore() {
        ___tranlocal.value = value;
    }
}
