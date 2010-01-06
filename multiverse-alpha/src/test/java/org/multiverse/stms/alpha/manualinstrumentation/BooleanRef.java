package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Transaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.*;
import org.multiverse.stms.alpha.mixins.FastAtomicObjectMixin;
import org.multiverse.templates.AtomicTemplate;

public class BooleanRef extends FastAtomicObjectMixin {

    public BooleanRef() {
        this(false);
    }

    @AtomicMethod
    public BooleanRef(final boolean value) {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) {
                BooleanRefTranlocal tranlocal = (BooleanRefTranlocal) ((AlphaTransaction) t).load(BooleanRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    @AtomicMethod
    public void set(boolean value) {
        BooleanRefTranlocal tranlocal = ((BooleanRefTranlocal) AlphaStmUtils.load(this));
        set(tranlocal, value);
    }

    @AtomicMethod(readonly = true)
    public boolean get() {
        BooleanRefTranlocal tranlocal = ((BooleanRefTranlocal) AlphaStmUtils.load(this));
        return get(tranlocal);
    }


    public void set(BooleanRefTranlocal tranlocal, boolean newValue) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }

        tranlocal.value = newValue;
    }

    public boolean get(BooleanRefTranlocal tranlocal) {
        return tranlocal.value;
    }

    @Override
    public AlphaTranlocal ___loadUpdatable(long version) {
        BooleanRefTranlocal origin = (BooleanRefTranlocal) ___load(version);
        if (origin == null) {
            return new BooleanRefTranlocal(this);
        } else {
            return new BooleanRefTranlocal(origin);
        }

    }
}

class BooleanRefTranlocal extends AlphaTranlocal {

    BooleanRefTranlocal ___origin;
    final BooleanRef ___atomicObject;
    boolean value;

    public BooleanRefTranlocal(BooleanRefTranlocal origin) {
        this.___origin = origin;
        this.value = origin.value;
        this.___atomicObject = origin.___atomicObject;
    }

    public BooleanRefTranlocal(BooleanRef atomicObject) {
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
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new BooleanRefTranlocalSnapshot(this);
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
}

class BooleanRefTranlocalSnapshot extends AlphaTranlocalSnapshot {

    final BooleanRefTranlocal ___tranlocal;
    final boolean value;

    public BooleanRefTranlocalSnapshot(BooleanRefTranlocal tranlocal) {
        this.___tranlocal = tranlocal;
        value = tranlocal.value;
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
