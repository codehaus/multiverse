package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

/**
 * access modifiers for fields are public because this object is used for testing purposes. For the instrumentation the
 * fields don't need to be this public.
 */
public class IntRefTranlocal extends AlphaTranlocal {

    public IntRef ___txObject;
    private IntRefTranlocal ___origin;

    public int value;

    public IntRefTranlocal(IntRefTranlocal origin) {
        this.___origin = origin;
        this.___txObject = origin.___txObject;
        this.value = origin.value;
    }

    public IntRefTranlocal(IntRef txObject) {
        this.___txObject = txObject;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new IntRefTranlocal(this);
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
    public IntRefTranlocalSnapshot takeSnapshot() {
        return new IntRefTranlocalSnapshot(this);
    }
}

class IntRefTranlocalSnapshot extends AlphaTranlocalSnapshot {

    final IntRefTranlocal ___tranlocal;
    final int value;

    public IntRefTranlocalSnapshot(IntRefTranlocal tranlocal) {
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
