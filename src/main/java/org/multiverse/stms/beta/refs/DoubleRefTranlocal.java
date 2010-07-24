package org.multiverse.stms.beta.refs;

import org.multiverse.stms.beta.ObjectPool;

/**
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class DoubleRefTranlocal extends Tranlocal{

    public final static DoubleRefTranlocal LOCKED = new DoubleRefTranlocal(null,true);

    public double value;

    public DoubleRefTranlocal(DoubleRef ref){
        super(ref, false);
    }

    public DoubleRefTranlocal(DoubleRef ref, boolean locked){
        super(ref, locked);
    }

    public DoubleRefTranlocal openForWrite(ObjectPool pool) {
        assert isCommitted;

        DoubleRef _ref = (DoubleRef)owner;
        DoubleRefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new DoubleRefTranlocal(_ref);
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
        DoubleRefTranlocal _read = (DoubleRefTranlocal)read;
        isDirty = value != _read.value;
        return isDirty;
    }
}
