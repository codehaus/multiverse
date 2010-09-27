package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.Function;
import org.multiverse.api.predicates.Predicate;
import org.multiverse.stms.beta.BetaObjectPool;

/**
 * The {@link Tranlocal} for the {@link BetaRef).
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class RefTranlocal<E> extends Tranlocal{

    public final static RefTranlocal LOCKED = new RefTranlocal(null,true);

    public E value;
    public Predicate<E>[] validators;    
    public CallableNode headCallable;

    public RefTranlocal(BetaRef ref){
        super(ref, false);
    }

    public RefTranlocal(BetaRef ref, boolean locked){
        super(ref, locked);
    }

    public RefTranlocal openForWrite(final BetaObjectPool pool) {
        assert isCommitted;

        BetaRef _ref = (BetaRef)owner;
        RefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new RefTranlocal(_ref);
        }

        tranlocal.read = this;
        tranlocal.validators = validators;
        tranlocal.value = value;
        return tranlocal;
    }

    public void evaluateCommutingFunctions(final BetaObjectPool  pool){
        assert isCommuting;

        RefTranlocal<E> tranlocal
            = (RefTranlocal<E>)read;
        value = tranlocal.value;

        CallableNode current = headCallable;
        do{
            Function<E> function =
                (Function<E>)current.function;
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

    public RefTranlocal openForCommute(final BetaObjectPool pool) {
        assert isCommitted;

        BetaRef _ref = (BetaRef)owner;
        RefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new RefTranlocal(_ref);
        }

        tranlocal.isCommuting = true;
        tranlocal.read = this;
        tranlocal.value = value;
        tranlocal.validators = validators;
        return tranlocal;
    }

    public void prepareForPooling(final BetaObjectPool pool) {
        owner = null;
        value = null;
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
        RefTranlocal _read = (RefTranlocal)read;
        isDirty = value != _read.value? DIRTY_TRUE: DIRTY_FALSE;

        return isDirty == DIRTY_TRUE;
    }
}
