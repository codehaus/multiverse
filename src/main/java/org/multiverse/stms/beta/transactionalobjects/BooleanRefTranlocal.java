package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.BooleanFunction;
import org.multiverse.api.functions.Function;
import org.multiverse.stms.beta.BetaObjectPool;

/**
 * The {@link Tranlocal} for the {@link BetaBooleanRef).
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class BooleanRefTranlocal extends Tranlocal{

    public final static BooleanRefTranlocal LOCKED = new BooleanRefTranlocal(null,true);

    public boolean value;
    public CallableNode headCallable;

    public BooleanRefTranlocal(BetaBooleanRef ref){
        super(ref, false);
    }

    public BooleanRefTranlocal(BetaBooleanRef ref, boolean locked){
        super(ref, locked);
    }

    public BooleanRefTranlocal openForWrite(final BetaObjectPool pool) {
        assert isCommitted;

        BetaBooleanRef _ref = (BetaBooleanRef)owner;
        BooleanRefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new BooleanRefTranlocal(_ref);
        }

        tranlocal.read = this;
        tranlocal.value = value;
        return tranlocal;
    }

    public void evaluateCommutingFunctions(final BetaObjectPool  pool){
        assert isCommuting;

        BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)read;
        value = tranlocal.value;

        CallableNode current = headCallable;
        do{
            value = current.callable.call(value);
            current = current.next;
        }while(current!=null);

        isDirty = tranlocal.value != value?DIRTY_TRUE : DIRTY_FALSE;
        isCommuting = false;
        headCallable = null;
    }

    public void addCommutingFunction(final BooleanFunction function, final BetaObjectPool pool){
        assert isCommuting;

        headCallable = new CallableNode(function, headCallable);
    }

    public void addCommutingFunction(final Function function, final BetaObjectPool pool){
        assert isCommuting;

        headCallable = new CallableNode(
            (BooleanFunction)function,
            headCallable);
    }

    public BooleanRefTranlocal openForCommute(final BetaObjectPool pool) {
        assert isCommitted;

        BetaBooleanRef _ref = (BetaBooleanRef)owner;
        BooleanRefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new BooleanRefTranlocal(_ref);
        }

        tranlocal.isCommuting = true;
        tranlocal.read = this;
        tranlocal.value = value;
        return tranlocal;
    }

    public void prepareForPooling(final BetaObjectPool pool) {
        owner = null;
        value = false;
        read = null;
        isCommitted = false;
        isDirty = DIRTY_UNKNOWN;
        isCommuting = false;
        //todo: this should be pooled.
        headCallable = null;
    }

    public boolean calculateIsDirty() {
        if(isDirty != DIRTY_UNKNOWN){
            return isDirty == DIRTY_TRUE;
        }

        //once committed, it never can become dirty (unless it is pooled and reused)
        if (isCommitted) {
            return false;
        }

        if (read == null) {
            //when the read is null, and it is an update, then is a tranlocal for a newly created
            //transactional object, since it certainly needs to be committed.
            isDirty = DIRTY_TRUE;
            return true;
        }

        //check if it really is dirty.
        BooleanRefTranlocal _read = (BooleanRefTranlocal)read;
        isDirty = value != _read.value? DIRTY_TRUE: DIRTY_FALSE;

        return isDirty == DIRTY_TRUE;
    }

    public static class CallableNode{
        public BooleanFunction callable;
        public CallableNode next;

        CallableNode(BooleanFunction callable, CallableNode next){
            this.callable = callable;
            this.next = next;
        }
    }
}
