package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.*;
import org.multiverse.stms.alpha.mixins.FastAtomicObjectMixin;
import org.multiverse.templates.AtomicTemplate;

/**
 * This implementation suffers from the ABA problem (well.. the stm suffers from it because the isDirty method suffers
 * from it). This can be fixed very easily, just add a counter. So although the references may not have changed in the
 * end but the counter has. And this will cause the writeconfict we are after for the ABA problem.  See the AbaRef.
 *
 * @author Peter Veentjer
 */
public class Ref<E> extends FastAtomicObjectMixin {

    public Ref() {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
                return null;
            }
        }.execute();
    }

    public Ref(final E value) {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public E get() {
        return new AtomicTemplate<E>(true) {
            @Override
            public E execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
                return get(tranlocal);
            }
        }.execute();
    }

    public void set(final E newValue) {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
                set(tranlocal, newValue);
                return null;
            }
        }.execute();
    }

    public boolean isNull() {
        return new AtomicTemplate<Boolean>(true) {
            @Override
            public Boolean execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
                return isNull(tranlocal);
            }
        }.execute();
    }

    public E clear() {
        return new AtomicTemplate<E>() {
            @Override
            public E execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).load(Ref.this);
                return clear(tranlocal);
            }
        }.execute();
    }

    @Override
    public RefTranlocal<E> ___loadUpdatable(long readVersion) {
        RefTranlocal<E> origin = (RefTranlocal) ___load(readVersion);
        if (origin == null) {
            return new RefTranlocal<E>(this);
        } else {
            return new RefTranlocal(origin);
        }
    }

    public E clear(RefTranlocal<E> tranlocal) {
        E oldValue = tranlocal.value;
        tranlocal.value = null;
        return oldValue;
    }

    public boolean isNull(RefTranlocal<E> tranlocal) {
        return tranlocal.value == null;
    }

    public E get(RefTranlocal<E> tranlocal) {
        return tranlocal.value;
    }

    public void set(RefTranlocal<E> tranlocal, E newValue) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }
        tranlocal.value = newValue;
    }
}

class RefTranlocal<E> extends AlphaTranlocal {

    Ref ___atomicObject;
    RefTranlocal ___origin;
    E value;


    RefTranlocal(RefTranlocal<E> origin) {
        this.___atomicObject = origin.___atomicObject;
        this.___origin = origin;
        this.value = origin.value;
    }

    RefTranlocal(Ref<E> atomicObject) {
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
        return new RefTranlocalSnapshot<E>(this);
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

class RefTranlocalSnapshot<E> extends AlphaTranlocalSnapshot {

    final RefTranlocal ___tranlocal;
    final E value;

    RefTranlocalSnapshot(RefTranlocal<E> tranlocal) {
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
