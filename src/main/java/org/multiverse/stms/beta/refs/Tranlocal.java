package org.multiverse.stms.beta.refs;

import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.BetaTransactionalObject;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Peter Veentjer
 */
public abstract class Tranlocal{

    public final static AtomicLong created = new AtomicLong();

    public final static Tranlocal LOCKED = new LongRefTranlocal(null);

    public BetaTransactionalObject owner;
    public boolean isPermanent;
    public boolean isCommitted;
    public boolean isDirty;

    public Tranlocal read;
    public final boolean locked;

    public Tranlocal(BetaTransactionalObject owner,boolean locked) {
        this.owner = owner;
        this.locked = locked;
        //created.incrementAndGet();
    }

    public abstract void clean();

    public final void prepareForCommit() {
        assert !isCommitted;
        this.isCommitted = true;
        this.read = null;
        this.isDirty = false;
    }

    public abstract Tranlocal openForWrite(ObjectPool pool);

    public abstract boolean calculateIsDirty();

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
}
