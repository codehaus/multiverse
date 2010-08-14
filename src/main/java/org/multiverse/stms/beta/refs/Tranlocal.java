package org.multiverse.stms.beta.refs;

import org.multiverse.durability.DurableObject;
import org.multiverse.durability.DurableState;
import org.multiverse.functions.Function;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaTransactionalObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Peter Veentjer
 */
public abstract class Tranlocal implements DurableState {

    public final static AtomicLong created = new AtomicLong();

    public BetaTransactionalObject owner;
    public boolean isPermanent;
    public boolean isCommitted;
    public boolean isCommuting;
    public boolean isDirty;
    public final boolean isLocked;

    public Tranlocal read;
    public long version;

    public Tranlocal(BetaTransactionalObject owner, boolean locked) {
        this.owner = owner;
        this.isLocked = locked;
        //created.incrementAndGet();
    }

    /**
     * Prepares the tranlocal for pooling so that it can be reused.
     *
     * @param pool the BetaObjectPool used for pooling. If there are any internal resources that can be pooled, this
     *             pool can be used for it.
     */
    public abstract void prepareForPooling(BetaObjectPool pool);

    public final void prepareForCommit() {
        assert !isCommitted;
        this.isCommitted = true;
        this.read = null;
        this.isDirty = false;
    }

    public abstract Tranlocal openForWrite(BetaObjectPool pool);

    public abstract Tranlocal openForCommute(BetaObjectPool pool);

    public abstract boolean calculateIsDirty();

    public abstract void evaluateCommutingFunctions(BetaObjectPool pool);

    public abstract void addCommutingFunction(Function function, BetaObjectPool pool);

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

    /**
     * Returns an iterator over all DurableObject that can be reached from this State.
     *
     * @return
     */
    public Iterator<DurableObject> getReferences() {
        return new LinkedList<DurableObject>().iterator();
    }

    @Override
    public BetaTransactionalObject getOwner() {
        return owner;
    }
}