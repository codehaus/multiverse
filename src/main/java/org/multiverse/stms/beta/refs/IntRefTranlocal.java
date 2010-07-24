package org.multiverse.stms.beta.refs;

import org.multiverse.stms.beta.ObjectPool;

/**
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class IntRefTranlocal extends Tranlocal{

    public final static IntRefTranlocal LOCKED = new IntRefTranlocal(null,true);

    public int value;

    public IntRefTranlocal(IntRef ref){
        super(ref, false);
    }

    public IntRefTranlocal(IntRef ref, boolean locked){
        super(ref, locked);
    }

    public IntRefTranlocal openForWrite(ObjectPool pool) {
        assert isCommitted;

        IntRef _ref = (IntRef)owner;
        IntRefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new IntRefTranlocal(_ref);
        }

        tranlocal.read = this;
        tranlocal.value = value;
        return tranlocal;
    }

    public void clean() {
        owner = null;
        value = 0;
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
        IntRefTranlocal _read = (IntRefTranlocal)read;
        isDirty = value != _read.value;
        return isDirty;
    }
}
