package org.multiverse.stms.beta.refs;

import org.multiverse.stms.beta.ObjectPool;

/**
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class RefTranlocal<E> extends Tranlocal{

    public final static RefTranlocal LOCKED = new RefTranlocal(null,true);

    public E value;

    public RefTranlocal(Ref ref){
        super(ref, false);
    }

    public RefTranlocal(Ref ref, boolean locked){
        super(ref, locked);
    }

    public RefTranlocal openForWrite(ObjectPool pool) {
        assert isCommitted;

        Ref _ref = (Ref)owner;
        RefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new RefTranlocal(_ref);
        }

        tranlocal.read = this;
        tranlocal.value = value;
        return tranlocal;
    }

    public void clean() {
        owner = null;
        value = null;
        read = null;
        isCommitted = false;
        isDirty = false;
    }

    public boolean calculateIsDirty() {
        //once committed, it never can become dirty (unless it is pooled and reused)
        if (isCommitted) {
            return false;
        }

        if (read == null) {
            //when the read is null, and it is an update, then is a tranlocal for a newly created
            //transactional object, since it certainly needs to be committed.
            isDirty = true;
            return true;
        }

        //check if it really is dirty.
        RefTranlocal _read = (RefTranlocal)read;
        isDirty = value != _read.value;
        return isDirty;
    }
}
