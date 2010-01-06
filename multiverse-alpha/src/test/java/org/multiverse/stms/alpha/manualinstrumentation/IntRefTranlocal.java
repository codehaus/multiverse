package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.DirtinessStatus;

/**
 * access modifiers for fields are public because this object is used for testing purposes. For the instrumentation the
 * fields don't need to be this public.
 */
public class IntRefTranlocal extends AlphaTranlocal {

    public IntRef ___atomicObject;
    public int value;
    private IntRefTranlocal ___origin;

    public IntRefTranlocal(IntRefTranlocal origin) {
        this.___origin = origin;
        this.___atomicObject = origin.___atomicObject;
        this.value = origin.value;
    }

    public IntRefTranlocal(IntRef atomicObject) {
        this.___atomicObject = atomicObject;
    }

    @Override
    public AlphaAtomicObject getAtomicObject() {
        return ___atomicObject;
    }


    @Override
    public void prepareForCommit(long writeVersion) {
        this.___writeVersion = writeVersion;
        this.___origin = null;
    }

    @Override
    public DirtinessStatus getDirtinessStatus() {
        if (___writeVersion > 0) {
            return DirtinessStatus.readonly;
        } else if (___origin == null) {
            return DirtinessStatus.fresh;
        } else if (___origin.value != this.value) {
            return DirtinessStatus.dirty;
        } else {
            return DirtinessStatus.clean;
        }
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
