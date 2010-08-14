package org.multiverse.stms.beta.refs;

import org.multiverse.functions.Function;
import org.multiverse.stms.beta.BetaObjectPool;

/**
 * The {@link Tranlocal} for the {@link E).
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class RefTranlocal<E> extends Tranlocal{

    public final static RefTranlocal LOCKED = new RefTranlocal(null,true);

    public E value;
    public CallableNode<E> headCallable;

    public RefTranlocal(Ref ref){
        super(ref, false);
    }

    public RefTranlocal(Ref ref, boolean locked){
        super(ref, locked);
    }

    public RefTranlocal openForWrite(final BetaObjectPool pool) {
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

    public void evaluateCommutingFunctions(final BetaObjectPool  pool){
        assert isCommuting;

        RefTranlocal<E> tranlocal = (RefTranlocal<E>)read;
        value = tranlocal.value;

        CallableNode<E> current = headCallable;
        do{
            value = current.callable.call(value);
            current = current.next;
        }while(current!=null);

        isDirty = tranlocal.value != value;
        isCommuting = false;
        headCallable = null;
    }

    public void addCommutingFunction(final Function function, final BetaObjectPool pool){
        assert isCommuting;

        headCallable = new CallableNode<E>(
            (Function)function,
            headCallable);
    }

    public RefTranlocal openForCommute(final BetaObjectPool pool) {
        assert isCommitted;

        Ref _ref = (Ref)owner;
        RefTranlocal tranlocal = pool.take(_ref);
        if (tranlocal == null) {
            tranlocal = new RefTranlocal(_ref);
        }

        tranlocal.isCommuting = true;
        tranlocal.read = this;
        tranlocal.value = value;
        return tranlocal;
    }

    public void prepareForPooling(final BetaObjectPool pool) {
        owner = null;
        value = null;
        read = null;
        isCommitted = false;
        isDirty = false;
        isCommuting = false;
        //todo: this should be pooled.
        headCallable = null;
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

    public static class CallableNode<E>{
        public Function<E> callable;
        public CallableNode<E> next;

        CallableNode(Function<E> callable, CallableNode<E> next){
            this.callable = callable;
            this.next = next;
        }
    }
}
