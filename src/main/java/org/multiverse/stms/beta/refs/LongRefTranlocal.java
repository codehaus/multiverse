package org.multiverse.stms.beta.refs;

import org.multiverse.stms.beta.ObjectPool;

/**
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class LongRefTranlocal extends Tranlocal{

    public final static LongRefTranlocal LOCKED = new LongRefTranlocal(null,true);

    public long value;

    public LongRefTranlocal(LongRef ref){
        super(ref, false);
    }

    public LongRefTranlocal(LongRef ref, boolean locked){
        super(ref, locked);
    }

    public LongRefTranlocal openForWrite(ObjectPool pool) {
        assert isCommitted;

        LongRef _ref = (LongRef)owner;
        LongRefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new LongRefTranlocal(_ref);
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
        LongRefTranlocal _read = (LongRefTranlocal)read;
        isDirty = value != _read.value;
        return isDirty;
    }
}
