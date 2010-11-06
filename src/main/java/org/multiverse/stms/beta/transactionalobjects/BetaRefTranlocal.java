package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.Function;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;


/**
 * The {@link BetaTranlocal} for the {@link BetaRef).
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class BetaRefTranlocal<E> extends BetaTranlocal{

    public E value;
    public E oldValue;

    public BetaRefTranlocal(BetaRef ref){
        super(ref);
    }

    public final void openForRead(int desiredLockMode) {
        if (tx.status != BetaTransaction.ACTIVE) {
            throw tx.abortOpenForRead(owner);
        }

        final BetaTransactionConfiguration config = tx.config;

        desiredLockMode = desiredLockMode >= config.readLockMode
             ? desiredLockMode
             : config.readLockMode;

        switch(status){
            case STATUS_CONSTRUCTING:
                return;
            case STATUS_NEW: {
                 BetaRef o = (BetaRef)owner;

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
            case STATUS_COMMUTING: {
                final boolean loadSuccess = ((BetaRef)owner).___load(
                config.spinCount, tx, LOCKMODE_COMMIT, this);

                if (!loadSuccess) {
                    tx.abort();
                    throw ReadWriteConflict.INSTANCE;
                }

                evaluateCommutingFunctions(tx.pool);
                return;
            }
            default:{
                BetaRef o = (BetaRef)owner;
                if (getLockMode() < desiredLockMode) {
                    boolean loadSuccess = o.___tryLockAndCheckConflict(
                        tx, config.spinCount, this, desiredLockMode == LOCKMODE_COMMIT);

                    if (!loadSuccess) {
                        tx.abort();
                        throw ReadWriteConflict.INSTANCE;
                    }
                }
            }
        }
    }

    public final void openForWrite(int desiredLockMode) {
        if (tx.status != BetaTransaction.ACTIVE) {
            throw tx.abortOpenForRead(owner);
        }

        final BetaTransactionConfiguration config = tx.config;

        if (config.readonly) {
            throw tx.abortOpenForWriteWhenReadonly(owner);
        }

        desiredLockMode = desiredLockMode >= config.readLockMode
             ? desiredLockMode
             : config.writeLockMode;

        switch(status){
            case STATUS_CONSTRUCTING:
                return;
            case STATUS_NEW: {
                BetaRef o = (BetaRef)owner;

                final boolean loadSuccess = o.___load(
                    config.spinCount, tx, desiredLockMode, this);

                if (!loadSuccess) {
                    tx.abort();
                    throw ReadWriteConflict.INSTANCE;
                }

                setStatus(STATUS_UPDATE);
                tx.hasUpdates = true;
                setIgnore(desiredLockMode == LOCKMODE_NONE && !hasDepartObligation());
                return;
            }
            case STATUS_COMMUTING: {
                final boolean loadSuccess = ((BetaRef)owner).___load(
                config.spinCount, tx, LOCKMODE_COMMIT, this);

                if (!loadSuccess) {
                    tx.abort();
                    throw ReadWriteConflict.INSTANCE;
                }

                evaluateCommutingFunctions(tx.pool);
                setStatus(STATUS_UPDATE);
                tx.hasUpdates = true;
                setIgnore(desiredLockMode == LOCKMODE_NONE && !hasDepartObligation());
                return;
            }
            default:{
                BetaRef o = (BetaRef)owner;
                setStatus(STATUS_UPDATE);
                tx.hasUpdates = true;
                if (getLockMode() < desiredLockMode) {
                    boolean loadSuccess = o.___tryLockAndCheckConflict(
                        tx, config.spinCount, this, desiredLockMode == LOCKMODE_COMMIT);

                    if (!loadSuccess) {
                        tx.abort();
                        throw ReadWriteConflict.INSTANCE;
                    }
                }
            }
        }
    }

    @Override
    public final void evaluateCommutingFunctions(final BetaObjectPool  pool){
        assert isCommuting();

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
        value = null;
        oldValue = null;
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
