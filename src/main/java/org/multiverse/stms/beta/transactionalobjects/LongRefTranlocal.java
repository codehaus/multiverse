package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.predicates.*;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;


/**
 * The {@link Tranlocal} for the {@link BetaLongRef).
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class LongRefTranlocal extends Tranlocal{

    public long value;
    public long oldValue;
    public LongPredicate[] validators;

    public LongRefTranlocal(BetaLongRef ref){
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
            BetaLongRef o = (BetaLongRef)owner;

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
            final boolean loadSuccess = ((BetaLongRef)owner).___load(
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

        long newValue = value;

        CallableNode current = headCallable;
        headCallable = null;
        do{
            LongFunction function =
                (LongFunction)current.function;
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
