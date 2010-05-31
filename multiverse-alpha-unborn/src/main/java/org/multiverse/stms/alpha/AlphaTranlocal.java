package org.multiverse.stms.alpha;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.Transaction;
import org.multiverse.api.commitlock.CommitLock;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.utils.TodoException;

import static java.lang.String.format;

/**
 * The Tranlocal is the Transaction local content of a TransactionalObject, since the state from the TransactionalObject
 * is removed. So for every TransactionalObject there are 1 or more Tranlocals (or zero when the TransactionalObject is
 * being constructed).
 * <p/>
 * Semantics of version: after the Tranlocal is committed, the version contains the write version. Before the commit it
 * contains the current read version.
 * <p/>
 * To support nested transactions there are partial rollbacks, each Tranlocal is able to make a snapshot of itself, so
 * that it can be restored when a (nested) transaction rolls back.
 * <p/>
 * Once the Tranlocal has been committed, the fields should only be read and not written. Since the publication (commit)
 * introduces some happens before relation, the fields in this object will also lift on that happens before relation.
 *
 * @author Peter Veentjer.
 */
public abstract class AlphaTranlocal implements CommitLock, MultiverseConstants {

    public final static long OPENED_FOR_WRITE = 0;
    public final static long OPENED_FOR_WRITE_AND_DIRTY = -1;
    public final static long OPENED_FOR_COMMUTE = -2;
    public final static long OPENED_FOR_COMMUTE_AND_DIRTY = -3;

    //larger than 0 means committed.
    public long ___writeVersion = 0;

    public AlphaTranlocal ___origin;

    public AlphaTransactionalObject ___transactionalObject;

    /**
     * Returns the AlphaTransactionalObject that belongs to this AlphaTranlocal.
     *
     * @return the AlphaTransactionalObject that belongs to this AlphaTranlocal
     */
    public final AlphaTransactionalObject getTransactionalObject() {
        return ___transactionalObject;
    }

    /**
     * Returns the original committed AlphaTranlocal this AlphaTranlocal is a updatable version of.
     *
     * @return the origin. Undefined after AlphaTranlocal is committed.
     */
    public final AlphaTranlocal getOrigin() {
        return ___origin;
    }

    /**
     * Creates the TranlocalSnapshot of the Tranlocal. A snapshot should only be made if this Tranlocal is not
     * committed.
     *
     * @return the snapshot.
     */
    public abstract AlphaTranlocalSnapshot takeSnapshot();

    /**
     * Returns a clone of this AlphaTranlocal to be used for updates. This method only should be called on tranlocals
     * that have committed (so have a ___writeVersion larger than 0). The implementation is free to check for this
     * violation, but it isn't mandatory.
     *
     * @return the clone of this AlphaTranlocal  that can be used for updates.
     */
    public abstract AlphaTranlocal openForWrite();

    /**
     * Is called just before this tranlocal commits. It allows the Tranlocal to do needed cleanup.
     * <p/>
     * An implementation needs to do at least 2 things: <ol> <li>change committed to true</li> <li>set the version to
     * the writeVersion</li> </ol>
     * <p/>
     * Detection if the writeVersion makes sense is not mandatory for the implementation.
     *
     * @param writeVersion the version of the commit. This is the version this tranlocal from now on will be known. It
     *                     is never going to change anymore.
     */
    public final void prepareForCommit(long writeVersion) {
        ___origin = null;
        ___writeVersion = writeVersion;
    }

    /**
     * Checks if this Tranlocal should be committed.
     *
     * @return true if it should be committed, false otherwise.
     */
    public abstract boolean isDirty();

    /**
     * Checks if the tranlocal is dirty and caches the value
     * <p/>
     * If the tranlocal is committed, false is returned and nothing is changed.
     * <p/>
     * If not is dirty (but also not committed) the ___writeVersion is set to 0 and false is returned.
     * <p/>
     * If dirty (but also not committed) the ___writeVersion is set to -1.
     *
     * @return
     */
    public final boolean executeDirtyCheck() {
        if (isCommitted()) {
            return false;
        }

        if (isCommuting()) {
            //todo: you now always 
            ___writeVersion = OPENED_FOR_COMMUTE_AND_DIRTY;
            return true;
        }

        if (isDirty()) {
            ___writeVersion = OPENED_FOR_WRITE_AND_DIRTY;
            return true;
        } else {
            ___writeVersion = OPENED_FOR_WRITE;
            return false;
        }
    }

    /**
     * Checks if the tranlocal is dirty by making use of a ___writeVersion that is changed by the executeDirtyCheck method.
     * If the tranlocal is committed, false will be returned.
     *
     * @return true if dirty, false otherwise.
     */
    public final boolean getPrecalculatedIsDirty() {
        return ___writeVersion == OPENED_FOR_COMMUTE_AND_DIRTY
                || ___writeVersion == OPENED_FOR_WRITE_AND_DIRTY
                || ___writeVersion == OPENED_FOR_COMMUTE;
    }

    public final boolean isCommitted() {
        return ___writeVersion > 0;
    }

    public final boolean isUncommitted() {
        return !isCommitted();
    }

    public final boolean isCommuting() {
        return ___writeVersion == OPENED_FOR_COMMUTE
                || ___writeVersion == OPENED_FOR_COMMUTE_AND_DIRTY;
    }

    public void prematureFixation(AlphaTransaction tx, AlphaTranlocal origin) {
        throw new TodoException();
    }

    //todo: inline

    public void lateFixation(AlphaTransaction tx) {
        if (isCommuting()) {
            throw new TodoException();
        }
    }

    public final long getWriteVersion() {
        return ___writeVersion;
    }

    public final boolean hasWriteConflict() {
        if (isCommuting()) {
            return false;
        }

        if (isCommitted()) {
            return false;
        }

        AlphaTranlocal current = ___transactionalObject.___load();
        return current != getOrigin();
    }

    public final boolean hasReadConflict(Transaction allowedLockOwner) {
        if (isCommuting()) {
            return false;
        }

        AlphaTransactionalObject txObject = getTransactionalObject();

        Transaction lockOwner = txObject.___getLockOwner();
        if (lockOwner != null && lockOwner != allowedLockOwner) {
            return true;
        }

        AlphaTranlocal current = txObject.___load();
        if (isCommitted()) {
            return current != this;
        }

        return current != getOrigin();
    }

    @Override
    public final Transaction ___getLockOwner() {
        return ___transactionalObject.___getLockOwner();
    }

    @Override
    public final boolean ___tryLock(Transaction lockOwner) {
        return ___transactionalObject.___tryLock(lockOwner);
    }

    @Override
    public final void ___releaseLock(Transaction expectedLockOwner) {
        ___transactionalObject.___releaseLock(expectedLockOwner);
    }

    /**
     * Debug representation of a TransactionalObject.
     *
     * @return the string representation of the atomicobject.
     */
    public String toDebugString() {
        if (isCommitted()) {
            return format("readonly-tranlocal(class=%s@%s, version=%s)",
                    getTransactionalObject().getClass().getSimpleName(),
                    System.identityHashCode(getTransactionalObject()),
                    ___writeVersion);
        } else {
            return format("update-tranlocal(origin=%s)",
                    getOrigin() == null ? null : getOrigin().toDebugString());
        }
    }
}
