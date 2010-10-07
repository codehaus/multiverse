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
 * The Tranlocal contains the transaction local state of a BetaTransactionalObject (so also the refs).
 * It only exists when a transaction is running, and when it commits, the values it contains will be
 * written to the BetaTransactionalObject if needed.
 *
 * @author Peter Veentjer
 */
public abstract class Tranlocal implements DurableState, BetaStmConstants {

    public BetaTransactionalObject owner;
    public int lockMode;
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
     * isDirty field. The call can be made more than once, but once it is marked as dirty, it will remain
     * dirty.
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

    public final boolean doPrepareWithWriteSkewPrevention(
            final BetaObjectPool pool, final BetaTransaction tx, final int spinCount, final boolean dirtyCheck) {

        if (isConstructing) {
            return true;
        }

        if (isCommitted) {
            if (lockMode == LOCKMODE_COMMIT) {
                return true;
            }
            return owner.___tryLockAndCheckConflict(tx, spinCount, this, true);

        }

        if (isCommuting) {
            if (owner.___load(spinCount, tx, LOCKMODE_COMMIT, this)) {
                return false;
            }

            evaluateCommutingFunctions(pool);
            return true;
        }

        if (dirtyCheck) {
            calculateIsDirty();
        }

        if (lockMode == LOCKMODE_COMMIT) {
            return true;
        }
        return owner.___tryLockAndCheckConflict(tx, spinCount, this, true);

    }

    public final boolean doPrepareDirtyUpdates(
            final BetaObjectPool pool, final BetaTransaction tx, final int spinCount) {

        if (isCommitted || isConstructing) {
            return true;
        }

        if (isCommuting) {
            if (!owner.___load(spinCount, tx, LOCKMODE_COMMIT, this)) {
                return false;
            }

            evaluateCommutingFunctions(pool);
            return true;
        }

        if (!(isDirty || calculateIsDirty())) {
            return true;
        }

        if (lockMode == LOCKMODE_COMMIT) {
            return true;

        }
        return owner.___tryLockAndCheckConflict(tx, spinCount, this, true);
    }

    public final boolean doPrepareAllUpdates(
            final BetaObjectPool pool, BetaTransaction tx, int spinCount) {

        if (isCommitted || isConstructing) {
            return true;
        }

        if (lockMode == LOCKMODE_COMMIT) {
            return true;
        }

        if (isCommuting) {
            if (!owner.___load(spinCount, tx, LOCKMODE_COMMIT, this)) {
                return false;
            }

            evaluateCommutingFunctions(pool);
            return true;
        }

        return owner.___tryLockAndCheckConflict(tx, spinCount, this, true);
    }

    public Iterator<DurableObject> getReferences() {
        return new LinkedList<DurableObject>().iterator();
    }
}
