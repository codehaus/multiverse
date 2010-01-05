package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

public class ManualRefTranlocal extends AlphaTranlocal {

    public ManualRef ___txObject;
    private ManualRefTranlocal ___origin;

    public int value;

    public ManualRefTranlocal(ManualRefTranlocal origin) {
        this.___origin = origin;
        this.___txObject = origin.___txObject;
        this.value = origin.value;
    }

    public ManualRefTranlocal(ManualRef txObject) {
        this.___txObject = txObject;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new ManualRefTranlocal(this);
    }

    @Override
    public AlphaTransactionalObject getTransactionalObject() {
        return ___txObject;
    }

    @Override
    public AlphaTranlocal getOrigin() {
        return ___origin;
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        this.___writeVersion = writeVersion;
        this.___origin = null;
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        if (___origin.value != value) {
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
