package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.Function;
import org.multiverse.durability.DurableObject;
import org.multiverse.durability.DurableState;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStmConstants;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Peter Veentjer
 */
public abstract class Tranlocal implements DurableState, BetaStmConstants {

    public final static Tranlocal LOCKED = new Tranlocal(null, true) {
        @Override
        public void prepareForPooling(BetaObjectPool pool) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tranlocal openForWrite(BetaObjectPool pool) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tranlocal openForCommute(BetaObjectPool pool) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean calculateIsDirty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void evaluateCommutingFunctions(BetaObjectPool pool) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addCommutingFunction(Function function, BetaObjectPool pool) {
            throw new UnsupportedOperationException();
        }
    };

    public BetaTransactionalObject owner;
    public boolean isPermanent;
    public boolean isCommitted;
    public boolean isCommuting;
    public int isDirty = DIRTY_UNKNOWN;
    public final boolean isLocked;
    public Tranlocal read;

    public Tranlocal(BetaTransactionalObject owner, boolean locked) {
        this.owner = owner;
        this.isLocked = locked;
    }

    /**
     * Prepares the tranlocal for pooling so that it can be reused.
     *
     * @param pool the BetaObjectPool used for pooling. If there are any internal resources that can be pooled, this
     *             pool can be used for it.
     */
    public abstract void prepareForPooling(BetaObjectPool pool);

    /**
     * Prepares this Tranlocal for committing. If there is a read, the read is getAndSet to null (to prevent
     * retaining an uncontrollable number of objects). Also the isDirty field is getAndSet to false and the isCommitted
     * field to false.
     * <p/>
     * No checks are done if the Tranlocal is in the committed state, so make sure that this is done from
     * the outside.
     */
    public final void prepareForCommit() {
        assert !isCommitted;
        this.isCommitted = true;
        this.read = null;
        this.isDirty = DIRTY_FALSE;
    }

    @Override
    public final BetaTransactionalObject getOwner() {
        return owner;
    }

    /**
     * Opens the Tranlocal for writing. This means that a copy is made that is updatable.
     *
     * @param pool the BetaObjectPool used to retrieve a Tranlocal
     * @return the opened Tranlocal.
     */
    public abstract Tranlocal openForWrite(BetaObjectPool pool);

    /**
     * Opens the Tranlocal for a commute operation. This means that an updatable copy is made that
     * is put in the 'isCommuting' state.
     *
     * @param pool the BetaObjectPool used to retrieve the Tranlocal
     * @return the opened Tranlocal.
     */
    public abstract Tranlocal openForCommute(BetaObjectPool pool);

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
        if (isPermanent) {
            return;
        }
        this.isPermanent = true;
    }

    public final boolean isPermanent() {
        return isPermanent;
    }

    public Iterator<DurableObject> getReferences() {
        return new LinkedList<DurableObject>().iterator();
    }
}