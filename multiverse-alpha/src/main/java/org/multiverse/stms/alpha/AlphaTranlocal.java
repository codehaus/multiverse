package org.multiverse.stms.alpha;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.utils.commitlock.CommitLock;
import org.multiverse.utils.commitlock.CommitLockResult;

import static java.lang.String.format;

/**
 * The Tranlocal is the Transaction local content of a AtomicObject, since the state from the AtomicObject is removed.
 * So for every AtomicObject there are 1 or more Tranlocals (or zero when the AtomicObject is being constructed).
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

    /**
     * Contains the write version
     * <p/>
     * 0 indicates not committed. positive indicates committed (so readonly).
     */
    public long ___writeVersion = 0;

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
    public abstract void prepareForCommit(long writeVersion);

    /**
     * Returns the AlphaAtomicObject that belongs to this AlphaTranlocal.
     *
     * @return the AlphaAtomicObject that belongs to this AlphaTranlocal
     */
    public abstract AlphaAtomicObject getAtomicObject();

    /**
     * Creates the TranlocalSnapshot of the Tranlocal. A snapshot should only be made if this Tranlocal is not
     * committed.
     *
     * @return the snapshot.
     */
    public abstract AlphaTranlocalSnapshot takeSnapshot();

    /**
     * Returns the DirtinessStatus for this Tranlocal. Based on this value the stm is able to decide what to do with an
     * Tranlocal.
     * <p/>
     * Value could be stale as soon as it is returned. It also depends on the implementation if the
     * DirtinessState.writeconflict is returned. If it isn't returned here, the transaction will abort eventually.
     *
     * @return The NeedToCommitState
     *
     * @throws org.multiverse.api.exceptions.LoadException
     *          if failed to load the data needed to perform the check on dirtiness state. Depends on the implementation
     *          if this is thrown.
     */
    public abstract DirtinessStatus getDirtinessStatus();

    @Override
    public final CommitLockResult tryLockAndDetectConflicts(Transaction lockOwner) {
        if (SANITY_CHECKS_ENABLED) {
            if (lockOwner == null) {
                throw new PanicError("tryLockAndDetectConflicts can't be called with null as lockOwner");
            }

            if (lockOwner.getStatus() != TransactionStatus.active) {
                String msg = format(
                        "Can't tryLockAndDetectConflicts with non active transaction '%s'",
                        lockOwner.getFamilyName());
                throw new PanicError(msg);
            }

            //check if this is only called when not committed?
        }

        AlphaAtomicObject atomicObject = getAtomicObject();

        boolean lockedAcquired = atomicObject.___tryLock(lockOwner);
        if (!lockedAcquired) {
            return CommitLockResult.failure;
        }

        AlphaTranlocal mostRecentlyWritten = atomicObject.___load();
        if (mostRecentlyWritten == null) {
            return CommitLockResult.success;
        }

        boolean noConflict = mostRecentlyWritten.___writeVersion <= lockOwner.getReadVersion();
        if (noConflict) {
            return CommitLockResult.success;
        }

        atomicObject.___releaseLock(lockOwner);
        return CommitLockResult.conflict;
    }

    @Override
    public final void releaseLock(Transaction expectedLockOwner) {
        if (SANITY_CHECKS_ENABLED) {
            if (expectedLockOwner == null) {
                throw new PanicError("releaseLock can't be called with null as expectedLockOwner");
            }

            if (expectedLockOwner.getStatus() != TransactionStatus.active) {
                String msg = format(
                        "Can't tryLockAndDetectConflicts with non active transaction '%s'",
                        expectedLockOwner.getFamilyName());
                throw new PanicError(msg);
            }
        }

        getAtomicObject().___releaseLock(expectedLockOwner);
    }
}
