package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.DoubleFunction;
import org.multiverse.api.functions.Function;
import org.multiverse.api.predicates.DoublePredicate;
import org.multiverse.stms.beta.BetaObjectPool;

/**
 * The {@link Tranlocal} for the {@link BetaDoubleRef).
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class DoubleRefTranlocal extends Tranlocal{

    public final static DoubleRefTranlocal LOCKED = new DoubleRefTranlocal(null,true);

    public double value;
    public DoublePredicate[] validators;    
    public CallableNode headCallable;

    public DoubleRefTranlocal(BetaDoubleRef ref){
        super(ref, false);
    }

    public DoubleRefTranlocal(BetaDoubleRef ref, boolean locked){
        super(ref, locked);
    }

    public DoubleRefTranlocal openForWrite(final BetaObjectPool pool) {
        assert isCommitted;

        BetaDoubleRef _ref = (BetaDoubleRef)owner;
        DoubleRefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new DoubleRefTranlocal(_ref);
        }

        tranlocal.read = this;
        tranlocal.validators = validators;
        tranlocal.value = value;
        return tranlocal;
    }

    public void evaluateCommutingFunctions(final BetaObjectPool  pool){
        assert isCommuting;

        DoubleRefTranlocal tranlocal
            = (DoubleRefTranlocal)read;
        value = tranlocal.value;

        CallableNode current = headCallable;
        do{
            DoubleFunction function =
                (DoubleFunction)current.function;
            value = function.call(value);
            CallableNode old = current;
            current = current.next;
            pool.putCallableNode(old);
        }while(current != null);

        isDirty = tranlocal.value != value ? DIRTY_TRUE : DIRTY_FALSE;
        isCommuting = false;
        headCallable = null;
    }

    public void addCommutingFunction(final Function function, final BetaObjectPool pool){
        assert isCommuting;

        CallableNode node = pool.takeCallableNode();
        if(node == null){
            headCallable = new CallableNode(function, headCallable);
        }else{
            node.function = function;
            node.next = headCallable;
            headCallable = node;
        }
    }

    public DoubleRefTranlocal openForCommute(final BetaObjectPool pool) {
        assert isCommitted;

        BetaDoubleRef _ref = (BetaDoubleRef)owner;
        DoubleRefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new DoubleRefTranlocal(_ref);
        }

        tranlocal.isCommuting = true;
        tranlocal.read = this;
        tranlocal.value = value;
        tranlocal.validators = validators;
        return tranlocal;
    }

    public void prepareForPooling(final BetaObjectPool pool) {
        owner = null;
        value = 0;
        read = null;
        isCommitted = false;
        isDirty = DIRTY_UNKNOWN;
        isCommuting = false;
        CallableNode current = headCallable;
        if(current!=null){
            headCallable = null;
            do{
                CallableNode next = current.next;
                pool.putCallableNode(current);
                current = next;
            }while(current!=null);
        }
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
        DoubleRefTranlocal _read = (DoubleRefTranlocal)read;
        isDirty = value != _read.value? DIRTY_TRUE: DIRTY_FALSE;

        return isDirty == DIRTY_TRUE;
    }
}
