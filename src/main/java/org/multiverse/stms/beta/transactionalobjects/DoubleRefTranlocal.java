package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.DoubleFunction;
import org.multiverse.api.functions.Function;
import org.multiverse.api.predicates.DoublePredicate;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;


/**
 * The {@link Tranlocal} for the {@link BetaDoubleRef).
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class DoubleRefTranlocal extends Tranlocal{

    public double value;
    public double oldValue;
    public DoublePredicate[] validators;

    public DoubleRefTranlocal(BetaDoubleRef ref){
        super(ref);
    }

    public final void openForRead(int desiredLockMode) {
        if (tx.status != BetaTransaction.ACTIVE) {
            throw tx.abortOpenForRead(owner);
        }

        if (isConstructing()) {
            return;
        }

        final BetaTransactionConfiguration config = tx.config;

        desiredLockMode = desiredLockMode >= config.readLockMode
                ? desiredLockMode
                : config.readLockMode;

        if (isNew()) {
            BetaDoubleRef o = (BetaDoubleRef)owner;

            final boolean loadSuccess = o.___load(
                config.spinCount, tx, desiredLockMode, this);

            if (!loadSuccess) {
                tx.abort();
                throw ReadWriteConflict.INSTANCE;
            }

            setStatus(STATUS_READONLY);
            setIgnore(desiredLockMode == LOCKMODE_NONE && !hasDepartObligation());
            return;
        }

        if (isCommuting()) {
            final boolean loadSuccess = ((BetaDoubleRef)owner).___load(
                config.spinCount, tx, LOCKMODE_COMMIT, this);

            if (!loadSuccess) {
                tx.abort();
                throw ReadWriteConflict.INSTANCE;
            }

            evaluateCommutingFunctions(tx.pool);
            return;
        }

        if (getLockMode() < desiredLockMode) {
            boolean loadSuccess = owner.___tryLockAndCheckConflict(
                    tx, config.spinCount, this, desiredLockMode == LOCKMODE_COMMIT);

            if (!loadSuccess) {
                tx.abort();
                throw ReadWriteConflict.INSTANCE;
            }
        }
    }

    @Override
    public final void evaluateCommutingFunctions(final BetaObjectPool  pool){
        assert isCommuting();

        double newValue = value;

        CallableNode current = headCallable;
        headCallable = null;
        do{
            DoubleFunction function =
                (DoubleFunction)current.function;
            newValue = function.call(newValue);

            CallableNode old = current;
            current = current.next;
            pool.putCallableNode(old);
        }while(current != null);

        value = newValue;
        setDirty(newValue != oldValue);
        setStatus(STATUS_UPDATE);
    }

    @Override
    public void addCommutingFunction(final Function function, final BetaObjectPool pool){
        assert isCommuting();

        CallableNode node = pool.takeCallableNode();
        node.function = function;
        node.next = headCallable;
        headCallable = node;
    }

    @Override
    public void prepareForPooling(final BetaObjectPool pool) {
        version = 0l;
        value = 0;
        oldValue = 0;
        owner = null;

        setLockMode(LOCKMODE_NONE);
        setDepartObligation(false);
        setStatus(STATUS_NEW);
        setDirty(false);
        setIsConflictCheckNeeded(false);

        tx = null;
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
        if(isDirty()){
            return true;
        }

        setDirty(value != oldValue);
        return isDirty();
    }
}
