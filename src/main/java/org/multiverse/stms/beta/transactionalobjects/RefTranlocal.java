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

    public E value;
    public E oldValue;
    public Predicate<E>[] validators;

    public RefTranlocal(BetaRef ref){
        super(ref);
    }

    @Override
    public final void evaluateCommutingFunctions(final BetaObjectPool  pool){
        assert isCommuting;

        E newValue = value;

        CallableNode current = headCallable;
        headCallable = null;
        do{
            Function<E> function =
                (Function<E>)current.function;
            newValue = function.call(newValue);

            CallableNode old = current;
            current = current.next;
            pool.putCallableNode(old);
        }while(current != null);

        value = newValue;
        isDirty = newValue != oldValue;
        isCommuting = false;
    }

    @Override
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

    @Override
    public void prepareForPooling(final BetaObjectPool pool) {
        version = 0l;
        value = null;
        oldValue = null;
        owner = null;
        lockMode = LOCKMODE_NONE;
        hasDepartObligation = false;
        isCommitted = false;
        isCommuting = false;
        isConstructing = false;
        isDirty = false;
        CallableNode current = headCallable;
        if (current != null) {
            headCallable = null;
            do {
                CallableNode next = current.next;
                pool.putCallableNode(current);
                current = next;
            } while (current != null);
      }
    }

    @Override
    public boolean calculateIsDirty() {
        if(isDirty){
            return true;
        }

        isDirty = value != oldValue;
        return isDirty;
    }
}
