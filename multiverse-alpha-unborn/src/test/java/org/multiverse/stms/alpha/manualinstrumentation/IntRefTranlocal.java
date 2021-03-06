package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;

/**
 * access modifiers for fields are public because this object is used for testing purposes. For the instrumentation the
 * fields don't need to be this public.
 */
public class IntRefTranlocal extends AlphaTranlocal {

    public int value;

    public IntRefTranlocal(IntRefTranlocal origin) {
        this.___origin = origin;
        this.___transactionalObject = origin.___transactionalObject;
        this.value = origin.value;
    }

    public IntRefTranlocal(IntRef txObject) {
        this.___transactionalObject = txObject;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new IntRefTranlocal(this);
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        IntRefTranlocal origin = (IntRefTranlocal) ___origin;
        if (origin.value != value) {
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
