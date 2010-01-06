package org.multiverse.stms.alpha.manualinstrumentation;

import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.*;
import org.multiverse.stms.alpha.mixins.FastAtomicObjectMixin;
import org.multiverse.templates.AtomicTemplate;

/**
 * @author Peter Veentjer
 */
public class LongRef extends FastAtomicObjectMixin {

    public LongRef(final long value) {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) {
                LongRefTranlocal tranlocal = (LongRefTranlocal) ((AlphaTransaction) t).load(LongRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    @AtomicMethod
    public void await(long expectedValue) {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.load(LongRef.this);
        await(tranlocal, expectedValue);
    }

    @AtomicMethod
    public void set(long value) {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.load(LongRef.this);
        set(tranlocal, value);
    }

    @AtomicMethod(readonly = true)
    public long get() {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.load(LongRef.this);
        return get(tranlocal);
    }

    @AtomicMethod
    public void inc() {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.load(LongRef.this);
        inc(tranlocal);
    }

    @AtomicMethod
    public void dec() {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.load(LongRef.this);
        dec(tranlocal);
    }

    @AtomicMethod
    public LongRef add(LongRef ref) {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.load(LongRef.this);
        return tranlocal.add(ref);
    }

    @Override
    public LongRefTranlocal ___loadUpdatable(long version) {
        LongRefTranlocal origin = (LongRefTranlocal) ___load(version);
        if (origin == null) {
            return new LongRefTranlocal(this);
        } else {
            return new LongRefTranlocal(origin);
        }
    }

    public void set(LongRefTranlocal tranlocal, long newValue) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }
        tranlocal.value = newValue;
    }

    public long get(LongRefTranlocal tranlocal) {
        return tranlocal.value;
    }

    public void inc(LongRefTranlocal tranlocal) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }
        tranlocal.value++;
    }

    public void dec(LongRefTranlocal tranlocal) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }
        tranlocal.value--;
    }

    public void await(LongRefTranlocal tranlocal, long expectedValue) {
        if (tranlocal.value != expectedValue) {
            retry();
        }
    }
}

class LongRefTranlocal extends AlphaTranlocal {

    private final LongRef ___atomicObject;
    LongRefTranlocal ___origin;
    public long value;

    public LongRefTranlocal(LongRefTranlocal origin) {
        this.___origin = origin;
        this.___writeVersion = origin.___writeVersion;
        this.___atomicObject = origin.___atomicObject;
        this.value = origin.value;
    }

    public LongRefTranlocal(LongRef atomicObject) {
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
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new LongRefTranlocalSnapshot(this);
    }

    public LongRef add(LongRef ref) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    public static LongRef add(LongRefTranlocal owner, LongRefTranlocal arg) {
        return owner.___atomicObject.add(owner.___atomicObject);
    }
}

class LongRefTranlocalSnapshot extends AlphaTranlocalSnapshot {

    final LongRefTranlocal ___tranlocal;
    final long value;

    LongRefTranlocalSnapshot(LongRefTranlocal tranlocal) {
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