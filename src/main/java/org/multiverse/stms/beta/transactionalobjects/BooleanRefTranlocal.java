package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.BooleanFunction;
import org.multiverse.api.functions.Function;
import org.multiverse.api.predicates.BooleanPredicate;
import org.multiverse.stms.beta.BetaObjectPool;

/**
 * The {@link Tranlocal} for the {@link BetaBooleanRef).
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class BooleanRefTranlocal extends Tranlocal{

    public boolean value;
    public boolean oldValue;
    public BooleanPredicate[] validators;

    public BooleanRefTranlocal(BetaBooleanRef ref){
        super(ref);
    }

    @Override
    public final void evaluateCommutingFunctions(final BetaObjectPool  pool){
        assert isCommuting;

        boolean newValue = value;

        CallableNode current = headCallable;
        headCallable = null;
        do{
            BooleanFunction function =
                (BooleanFunction)current.function;
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
        value = false;
        oldValue = false;
        owner = null;
        lockMode = LOCKMODE_NONE;
        hasDepartObligation = false;
        isCommitted = false;
        isCommuting = false;
        isConstructing = false;
        isDirty = false;
        checkConflict = false;
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
