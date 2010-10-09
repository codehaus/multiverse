package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.Function;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.api.predicates.IntPredicate;
import org.multiverse.stms.beta.BetaObjectPool;

/**
 * The {@link Tranlocal} for the {@link BetaIntRef).
 * <p/>
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class IntRefTranlocal extends Tranlocal {

    public int value;
    public int oldValue;
    public IntPredicate[] validators;

    public IntRefTranlocal(BetaIntRef ref) {
        super(ref);
    }

    @Override
    public final void evaluateCommutingFunctions(final BetaObjectPool pool) {
        assert isCommuting;

        int newValue = value;

        CallableNode current = headCallable;
        headCallable = null;
        do {
            IntFunction function =
                    (IntFunction) current.function;
            newValue = function.call(newValue);

            CallableNode old = current;
            current = current.next;
            pool.putCallableNode(old);
        } while (current != null);

        value = newValue;
        isDirty = newValue != oldValue;
        isCommuting = false;
    }

    @Override
    public void addCommutingFunction(final Function function, final BetaObjectPool pool) {
        assert isCommuting;

        CallableNode node = pool.takeCallableNode();
        if (node == null) {
            headCallable = new CallableNode(function, headCallable);
        } else {
            node.function = function;
            node.next = headCallable;
            headCallable = node;
        }
    }

    @Override
    public void prepareForPooling(final BetaObjectPool pool) {
        version = 0l;
        value = 0;
        oldValue = 0;
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
        if (isDirty) {
            return true;
        }

        isDirty = value != oldValue;
        return isDirty;
    }
}
