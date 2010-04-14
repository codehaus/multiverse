package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.Listeners;
import org.multiverse.api.exceptions.LockNotFreeWriteConflict;
import org.multiverse.api.exceptions.OptimisticLockFailedWriteConflict;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.api.exceptions.WriteSkewConflict;
import org.multiverse.stms.AbstractTransactionSnapshot;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AbstractAlphaTransaction;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.AlphaStmUtils.toTxObjectString;

/**
 * An abstract {@link org.multiverse.stms.alpha.transactions.AlphaTransaction} that provides all the pluming logic for
 * update transactions.
 *
 * @author Peter Veentjer.
 */
public abstract class AbstractUpdateAlphaTransaction
        extends AbstractAlphaTransaction<UpdateConfiguration, AbstractTransactionSnapshot> {

    private long writeVersion;

    //todo: for the time being
    protected UpdateTransactionStatus updateTransactionStatus = UpdateTransactionStatus.nowrites;

    public AbstractUpdateAlphaTransaction(UpdateConfiguration config) {
        super(config);
    }

    // ======================= clear =============================

    @Override
    protected final void doClear() {
        updateTransactionStatus = UpdateTransactionStatus.nowrites;
        dodoClear();
    }

    protected abstract void dodoClear();

    // ======================= attach operations =========================

    /**
     * Attaches the tranlocal to this Transaction. The tranlocal will never be null and this call is only made when no
     * read for the transactional object of the readonly has been made. It is important that an implementation doesn't
     * ignore this call.
     *
     * @param opened the opened AlphaTranlocal to attach.
     */
    protected abstract void attach(AlphaTranlocal opened);

    /**
     * Finds the tranlocal for the given transactional object in the set of attached tranlocals.
     *
     * @param txObject the transactional object to find the tranlocal for.
     * @return the found tranlocal. If no tranlocal was found in the set of attached tranlocals, null is returned.
     */
    protected abstract AlphaTranlocal findAttached(AlphaTransactionalObject txObject);

    // ======================= open for read =============================

    @Override
    protected final AlphaTranlocal doOpenForRead(AlphaTransactionalObject txObject) {
        AlphaTranlocal opened = findAttached(txObject);

        if (opened != null) {
            if (opened.isCommuting()) {
                AlphaTranlocal origin = txObject.___load(getReadVersion());
                if (origin == null) {
                    throw new UncommittedReadConflict();
                }

                opened.fixatePremature(this, origin);
            }

            return opened;
        }

        opened = txObject.___load(getReadVersion());
        if (opened == null) {
            throw new UncommittedReadConflict();
        } else if (config.automaticReadTrackingEnabled) {
            attach(opened);
        }

        return opened;
    }

    // ======================= open for write =============================

    @Override
    protected AlphaTranlocal doOpenForWrite(AlphaTransactionalObject txObject) {
        AlphaTranlocal attached = findAttached(txObject);
        if (attached == null) {
            attached = doOpenForWritePreviousCommittedAndAttach(txObject);
            updateTransactionStatus = updateTransactionStatus.upgradeToOpenForWrite();
        } else if (attached.isCommitted()) {
            //it is loaded before but it is a readonly
            //make an updatable clone of the tranlocal already is committed and use that
            //from now on.
            attached = attached.openForWrite();
            attach(attached);
            updateTransactionStatus = updateTransactionStatus.upgradeToOpenForWrite();
        } else if (attached.isCommuting()) {
            AlphaTranlocal origin = txObject.___load(getReadVersion());
            if (origin == null) {
                throw new UncommittedReadConflict();
            }

            attached.fixatePremature(this, origin);
            updateTransactionStatus = updateTransactionStatus.upgradeToOpenForWrite();
        }

        return attached;
    }

    //todo: this method is going to be inlined.

    protected final AlphaTranlocal doOpenForWritePreviousCommittedAndAttach(AlphaTransactionalObject txObject) {
        AlphaTranlocal committed = txObject.___load(getReadVersion());

        if (committed == null) {
            throw new UncommittedReadConflict();
        }

        AlphaTranlocal opened = committed.openForWrite();
        attach(opened);
        return opened;
    }


    // ======================= open for commuting write =============================

    @Override
    protected AlphaTranlocal doOpenForCommutingWrite(AlphaTransactionalObject txObject) {
        updateTransactionStatus = updateTransactionStatus.upgradeToOpenForWrite();

        AlphaTranlocal attached = findAttached(txObject);
        if (attached == null) {
            attached = txObject.___openForCommutingOperation();
            attach(attached);
        } else if (attached.isCommitted()) {
            attached = attached.openForWrite();
            attach(attached);
        }

        return attached;
    }

    // ======================= open for construction =============================

    @Override
    public final AlphaTranlocal doOpenForConstruction(AlphaTransactionalObject txObject) {
        AlphaTranlocal opened = findAttached(txObject);

        if (opened != null) {
            if (opened.isCommitted()) {
                String msg = format(
                        "Can't open for construction transactional object '%s' using transaction '%s'" +
                                "because the transactional object already has commits",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new IllegalStateException(msg);
            }

            if (opened.isCommuting()) {
                String msg = format(
                        "Can't open for construction transactional object '%s' using transaction '%s'" +
                                "because the transactional object is opened for a commuting operations",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new IllegalStateException(msg);
            }

            if (opened.getOrigin() != null) {
                String msg = format(
                        "Can't open for construction transactional object '%s' using transaction '%s'" +
                                "because the transactional object already has commits",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new IllegalStateException(msg);
            }

            return opened;
        }

        updateTransactionStatus = updateTransactionStatus.upgradeToOpenForConstruction();

        AlphaTranlocal fresh = txObject.___openUnconstructed();
        attach(fresh);
        return fresh;
    }

    // ======================= prepare/commit =============================

    @Override
    protected final void doPrepare() {
        switch (updateTransactionStatus) {
            case nowrites:
                //we are done
                break;
            case newonly:
                //if there are only fresh object, nothing needs to be locked and
                //the write version doesn't need to be increased.
                writeVersion = config.clock.getVersion();
                break;
            case updates:
                if (!isDirty()) {
                    return;
                }

                if (!tryWriteLocks()) {
                    throw createFailedToObtainCommitLocksException();
                }

                boolean hasConflict = false;
                try {
                    if (config.writeSkewProblemAllowed) {
                        if (config.optimizedConflictDetectionEnabled && getReadVersion() == config.clock.getVersion()) {
                            writeVersion = config.clock.tick();
                            //it could be that a different transaction also reached this part, so we need to make sure
                            hasConflict = writeVersion != getReadVersion() + 1;
                        } else {
                            hasConflict = hasWriteConflict();
                            if (!hasConflict) {
                                writeVersion = config.clock.tick();
                            }
                        }

                        if (hasConflict) {
                            throw createOptimisticLockFailedWriteConflict();
                        }
                    } else {
                        //todo: could here be a potential race problem because the reads are not locked, only the writes.
                        writeVersion = config.clock.strictTick();
                        hasConflict = hasReadConflict();

                        if (hasConflict) {
                            throw createWriteSkewConflict();
                        }
                    }
                } finally {
                    if (hasConflict) {
                        doReleaseWriteLocksForFailure();
                    }
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Returns the state of the attached tranlocals. This information is needed for the transaction to decide what to do
     * (write changes etc). It is important that the implementation should not stop after it finds one element that is
     * dirty, it should check all attached objects. This is because the isDirty flag on each tranlocal needs to be
     * cached for later use.
     *
     * @return the AttachedState.
     */
    protected abstract boolean isDirty();

    protected final boolean isDirty(AlphaTranlocal attached) {
        if (attached == null) {
            return false;
        }

        if (attached.isCommitted()) {
            return false;
        }

        if (!config.dirtyCheckEnabled) {
            return true;
        }

        return attached.isDirtyAndCacheValue();
    }

    /**
     * Checks if attached updated-items have a conflict. Call is made after the locks on the updates have been acquired.
     * A conflict means that a new version of the tranlocal (that itself is a update) already is stored in in the
     * transactional object.
     *
     * @return true if at least 1 attached tranlocal has a conflict.
     */
    protected abstract boolean hasWriteConflict();

    protected final boolean hasWriteConflict(AlphaTranlocal tranlocal) {
        if (tranlocal == null) {
            return false;
        }

        return tranlocal.hasWriteConflict();
    }

    /**
     * Checks if the reads don't have conflicts. The writes are neglected because they will be checked with the
     * hasWriteConflict.
     *
     * @return true if there are conflict, false otherwise.
     */
    protected abstract boolean hasReadConflict();

    protected final boolean hasReadConflict(AlphaTranlocal attached) {
        if (attached == null) {
            return false;
        }

        return attached.hasReadConflict(this);
    }

    /**
     * Locks the writeset. If the locks could not be obtained, false is returned.
     * <p/>
     * todo: in the future the dirty check could be added. If something is an update, but not dirty there is no need to
     * lock it under certain conditions (it causes the aba problem).
     *
     * @return true if the writeset was locked successfully, false otherwise.
     */
    protected abstract boolean tryWriteLocks();

    /**
     * Releases all the locks on the transactional objects of the attached tranlocals that have been acquired. This call
     * is made when the commit was not a success (so the writeversion still isn't set).
     * <p/>
     * It is important that this call doesn't fail, because else it could leave transactional objects locked forever.
     */
    protected abstract void doReleaseWriteLocksForFailure();

    protected final void doReleaseWriteSetLocksForFailure(AlphaTranlocal tranlocal) {
        boolean release = true;

        if (tranlocal == null) {
            release = false;
        } else if (tranlocal.isCommitted()) {
            release = false;
        } else if (config.dirtyCheckEnabled && !tranlocal.getPrecalculatedIsDirty()) {
            release = false;
        }

        if (release) {
            AlphaTransactionalObject txObject = tranlocal.getTransactionalObject();
            txObject.___releaseLock(this);
        }
    }

    /**
     * Releases the locks on the tranlocal objects of the attached tranlocals with the given writeVersion. This call is
     * made when the commit was a success.
     * <p/>
     * It is important that this call doesn't fail, because else it could leave transactional objects locked forever.
     *
     * @param writeVersion the writeVersion of the tranlocals to release.
     */
    protected abstract void doReleaseWriteLocksForSuccess(long writeVersion);

    protected final void doReleaseWriteLockForSuccess(AlphaTranlocal tranlocal, long writeVersion) {
        if (tranlocal == null) {
            return;
        }

        boolean release = true;

        if (tranlocal.isCommitted()) {
            if (tranlocal.___writeVersion != writeVersion) {
                release = false;
            }
        }

        if (release) {
            AlphaTransactionalObject txObject = tranlocal.getTransactionalObject();
            txObject.___releaseLock(this);
        }
    }

    /**
     * Writes the tranlocals that need to be written to their transactional objects.
     * <p/>
     * It is important that a store always completes. If it doesn't it could cause partially committed transactions.
     *
     * @param writeVersion the version of the commit.
     * @return the array of listeners that need to be notified of the write (array is allowed to be null
     *         to prevent object creation).
     */
    protected abstract Listeners[] makeChangesPermanent(long writeVersion);

    protected final Listeners makePermanent(AlphaTranlocal tranlocal, long writeVersion) {
        if (tranlocal == null) {
            return null;
        }

        if (tranlocal.isCommitted()) {
            return null;
        }

        tranlocal.ifCommutingThenFixate(this);

        AlphaTransactionalObject txObject = tranlocal.getTransactionalObject();

        AlphaTranlocal origin = tranlocal.getOrigin();

        boolean store = false;
        if (!config.dirtyCheckEnabled) {
            store = true;
        } else {

            if (origin == null) {
                store = true;
            } else if (tranlocal.getPrecalculatedIsDirty()) {
                store = true;
            }
        }

        if (!store) {
            return null;
        }

        if (origin == null) {
            txObject.___storeInitial(tranlocal, writeVersion);
            return null;
        }

        return txObject.___storeUpdate(tranlocal, writeVersion, config.quickReleaseLocksEnabled);
    }

    @Override
    protected void doAbortPrepared() {
        doReleaseWriteLocksForFailure();
    }

    @Override
    protected void makeChangesPermanent() {
        Listeners[] listeners = makeChangesPermanent(writeVersion);
        if (!config.quickReleaseLocksEnabled) {
            doReleaseWriteLocksForSuccess(writeVersion);
        }
        Listeners.openAll(listeners);
    }

    // ======================= building exceptions ============================

    private OptimisticLockFailedWriteConflict createOptimisticLockFailedWriteConflict() {
        if (OptimisticLockFailedWriteConflict.reuse) {
            return OptimisticLockFailedWriteConflict.INSTANCE;
        }

        String msg = format(
                "Failed to commit transaction '%s' because there was a write conflict'",
                config.getFamilyName());
        return new OptimisticLockFailedWriteConflict(msg);
    }

    private WriteSkewConflict createWriteSkewConflict() {
        if (WriteSkewConflict.reuse) {
            return WriteSkewConflict.INSTANCE;
        }

        String msg = format(
                "Failed to commit transaction '%s' because a writeconflict was detected. " +
                        "The exact problem was a writeskew.",
                config.getFamilyName());
        return new WriteSkewConflict(msg);
    }

    private LockNotFreeWriteConflict createFailedToObtainCommitLocksException() {
        if (LockNotFreeWriteConflict.reuse) {
            return LockNotFreeWriteConflict.INSTANCE;
        }

        String msg = format(
                "Failed to commit transaction '%s' because not all the locks on the transactional objects " +
                        "in the writeset could be obtained",
                config.getFamilyName());
        return new LockNotFreeWriteConflict(msg);
    }
}
