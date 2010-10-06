package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.Function;
import org.multiverse.durability.DurableObject;
import org.multiverse.durability.DurableState;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Peter Veentjer
 */
public abstract class Tranlocal implements DurableState, BetaStmConstants {

    public BetaTransactionalObject owner;
    public boolean isLockOwner;
    public boolean hasDepartObligation;
    public boolean isCommitted;
    public boolean isCommuting;
    public boolean isConstructing;
    public boolean isDirty;
    public long version;
    public CallableNode headCallable;

    public Tranlocal(BetaTransactionalObject owner) {
        this.owner = owner;
    }

    public abstract void prepareForPooling(final BetaObjectPool pool);

    @Override
    public final BetaTransactionalObject getOwner() {
        return owner;
    }


    /**
     * Calculates if this Tranlocal is dirty (so needs to be written) and stores the result in the
     * isDirty field.
     * <p/>
     * todo: say something about repeated calls.
     *
     * @return true if dirty, false otherwise.
     */
    public abstract boolean calculateIsDirty();

    /**
     * Evaluates the commuting functions that are applied to this Tranlocal. This call is made under the
     * assumption that the Tranlocal is not committed, is in the 'isCommuting' mode and that the read
     * field has been getAndSet. If there is a change, the isDirty field also is getAndSet.
     *
     * @param pool the BetaObjectPool used to pool resources.
     */
    public abstract void evaluateCommutingFunctions(BetaObjectPool pool);

    /**
     * Adds a Function for commute to this Tranlocal. This call is made under the assumption that
     * the Tranlocal is not committed and in the 'isCommuting' mode.
     * <p/>
     * No checks on the Function are done, so no null check or check if the Function already is added.
     *
     * @param function the Function to add.
     * @param pool     the BetaObjectPool that can be used to pool resources for this operation.
     */
    public abstract void addCommutingFunction(Function function, BetaObjectPool pool);

    /**
     * Checks if this Tranlocal is committed.
     *
     * @return true if committed, false otherwise.
     */
    public final boolean isCommitted() {
        return isCommitted;
    }

    /**
     * Marks the tranlocal as read biased. The consequence of a read biased tranlocal is that no tracking
     * is done on the number of reads (so arrives/departs are ignored). This is important for the tranlocal
     * pooling; a read biased tranlocal can't be
     * <p/>
     * Once marked as permanent, it will never change.
     */
    public final void markAsPermanent() {
        if (hasDepartObligation) {
            return;
        }
        this.hasDepartObligation = true;
    }

    public final boolean doPrepareDirtyUpdates(final BetaObjectPool pool, BetaTransaction tx, int spinCount) {
        if (isCommitted || isConstructing) {
            return true;
        }

        if (isCommuting) {
            if (!owner.___load(spinCount, tx, LOCKMODE_COMMIT, this)) {
                return false;
            }
            evaluateCommutingFunctions(pool);
        } else if (!(isDirty || calculateIsDirty())) {
            return true;
        } else if (!owner.___tryLockAndCheckConflict(tx, spinCount, this, true)) {
            return false;
        }

        return true;
    }

    public final boolean doPrepareAllUpdates(final BetaObjectPool pool, BetaTransaction tx, int spinCount) {
        if (isCommitted || isConstructing) {
            return true;
        }

        if (isCommuting) {
            if (!owner.___load(spinCount, tx, LOCKMODE_COMMIT, this)) {
                return false;
            }
            evaluateCommutingFunctions(pool);
        } else if (!owner.___tryLockAndCheckConflict(tx, spinCount, this, true)) {
            return false;
        }

        return true;
    }

    public final boolean isHasDepartObligation() {
        return hasDepartObligation;
    }

    public Iterator<DurableObject> getReferences() {
        return new LinkedList<DurableObject>().iterator();
    }
}
