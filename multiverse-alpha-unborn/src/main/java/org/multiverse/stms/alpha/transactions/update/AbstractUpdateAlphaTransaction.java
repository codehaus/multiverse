package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.Listeners;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.commitlock.CommitLockFilter;
import org.multiverse.api.exceptions.LockNotFreeWriteConflict;
import org.multiverse.api.exceptions.OptimisticLockFailedWriteConflict;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.api.exceptions.WriteSkewConflict;
import org.multiverse.stms.AbstractTransactionSnapshot;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.UncommittedFilter;
import org.multiverse.stms.alpha.transactions.AbstractAlphaTransaction;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.AlphaStmUtils.toTxObjectString;

/**
 * An abstract {@link org.multiverse.stms.alpha.transactions.AlphaTransaction} that provides all the pluming
 * logic for update transactions.
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
    protected final void doReset() {
        updateTransactionStatus = UpdateTransactionStatus.nowrites;
        doDoReset();
    }

    protected abstract void doDoReset();

    // ======================= attach operations =========================

    /**
     * Attaches the tranlocal to this Transaction. The tranlocal will never be null and this call is only made when no
     * read for the transactional object of the readonly has been made. It is important that an implementation doesn't
     * ignore this call.
     *
     * @param tranlocal the opened AlphaTranlocal to attach.
     */
    protected abstract void attach(AlphaTranlocal tranlocal);

    /**
     * Finds the tranlocal for the given transactional object in the set of attached tranlocals.
     *
     * @param transactionalObject the transactional object to find the tranlocal for.
     * @return the found tranlocal. If no tranlocal was found in the set of attached tranlocals, null is returned.
     */
    protected abstract AlphaTranlocal findAttached(AlphaTransactionalObject transactionalObject);

    // ======================= open for read =============================

    @Override
    protected final AlphaTranlocal doOpenForRead(AlphaTransactionalObject transactionalObject) {
        AlphaTranlocal tranlocal = findAttached(transactionalObject);

        if (tranlocal != null) {
            if (tranlocal.isCommuting()) {
                AlphaTranlocal origin = load(transactionalObject);

                if (origin == null) {
                    throw createUncommittedException(transactionalObject);
                }

                tranlocal.prematureFixation(this, origin);
            }

            return tranlocal;
        }

        tranlocal = load(transactionalObject);
        if (tranlocal == null) {
            throw createUncommittedException(transactionalObject);
        } else if (config.readTrackingEnabled) {
            attach(tranlocal);
        }

        return tranlocal;
    }

   // ======================= open for write =============================

    @Override
    protected AlphaTranlocal doOpenForWrite(AlphaTransactionalObject transactionalObject) {
        AlphaTranlocal attached = findAttached(transactionalObject);
        if (attached == null) {
            attached = doOpenForWriteAndAttach(transactionalObject);
            updateTransactionStatus = updateTransactionStatus.upgradeToOpenForWrite();
        } else if (attached.isCommitted()) {
            //it is loaded before but it is a readonly
            //make an updatable clone of the tranlocal already is committed and use that
            //from now on.
            attached = attached.openForWrite();
            attach(attached);
            updateTransactionStatus = updateTransactionStatus.upgradeToOpenForWrite();
        } else if (attached.isCommuting()) {
            AlphaTranlocal origin = load(transactionalObject);
            if (origin == null) {
                throw createUncommittedException(transactionalObject);
            }

            attached.prematureFixation(this, origin);
            updateTransactionStatus = updateTransactionStatus.upgradeToOpenForWrite();
        }

        return attached;
    }

    //todo: this method is going to be inlined.

    protected final AlphaTranlocal doOpenForWriteAndAttach(AlphaTransactionalObject transactionalObject) {
        AlphaTranlocal committed = transactionalObject.___load(getReadVersion());

        if (committed == null) {
            throw new UncommittedReadConflict();
        }

        AlphaTranlocal opened = committed.openForWrite();
        attach(opened);
        return opened;
    }

    // ======================= open for commuting write =============================

    @Override
    protected AlphaTranlocal doOpenForCommutingWrite(AlphaTransactionalObject transactionalObject) {
        if (getStatus() == TransactionStatus.New) {
            start();
        }

        updateTransactionStatus = updateTransactionStatus.upgradeToOpenForWrite();

        AlphaTranlocal attached = findAttached(transactionalObject);
        if (attached == null) {
            attached = transactionalObject.___openForCommutingOperation();
            attach(attached);
        } else if (attached.isCommitted()) {
            attached = attached.openForWrite();
            attach(attached);
        }

        return attached;
    }

    // ======================= open for construction =============================

    @Override
    public final AlphaTranlocal doOpenForConstruction(AlphaTransactionalObject transactionalObject) {
        AlphaTranlocal opened = findAttached(transactionalObject);

        if (opened != null) {
            if (opened.isCommitted()) {
                String msg = format(
                        "Can't open for construction transactional object '%s' using transaction '%s'" +
                                "because the transactional object already has commits",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new IllegalStateException(msg);
            }

            if (opened.isCommuting()) {
                String msg = format(
                        "Can't open for construction transactional object '%s' using transaction '%s'" +
                                "because the transactional object is opened for a commuting operations",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new IllegalStateException(msg);
            }

            if (opened.getOrigin() != null) {
                String msg = format(
                        "Can't open for construction transactional object '%s' using transaction '%s'" +
                                "because the transactional object already has commits",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new IllegalStateException(msg);
            }

            return opened;
        }

        updateTransactionStatus = updateTransactionStatus.upgradeToOpenForConstruction();

        AlphaTranlocal fresh = transactionalObject.___openUnconstructed();
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

                CommitLockFilter commitLockFilter = config.dirtyCheckEnabled ? UncommittedFilter.DIRTY_CHECK : UncommittedFilter.NO_DIRTY_CHECK;

                if (!tryWriteLocks(commitLockFilter)) {
                    throw createFailedToObtainCommitLocksException();
                }

                boolean failure = true;
                try {
                    if (config.writeSkewAllowed) {
                        boolean skipConflictDetection = config.optimizedConflictDetectionEnabled
                                && getReadVersion() == config.clock.getVersion();

                        if (!skipConflictDetection) {

                            //we don't need to check for conflicts because:
                            //- writeskew is allowed (so we don't need to check reads for conflicts)
                            //- and the dirty tranlocals are locked
                            //- and no other transaction committed after this transaction started,
                            //Based on these 3 arguments we can conclude that 
                            if (hasWriteConflict()) {
                                throw createOptimisticLockFailedWriteConflict();
                            }
                        }

                        writeVersion = config.clock.tick();
                    } else {
                        boolean possiblySkipConflictDetection = config.optimizedConflictDetectionEnabled
                                && getReadVersion() == config.clock.getVersion();

                        if (possiblySkipConflictDetection) {
                            //if the clock hasn't changed yet, there is a chance we can skip
                            //conflict detection

                            writeVersion = config.clock.strictTick();

                            if (writeVersion != getReadVersion() + 1) {
                                //we were not lucky, another transaction committed between the start and
                                //prepare of this transaction, so we need to do a conflict test,

                                if (hasReadWriteConflict()) {
                                    throw createWriteSkewConflict();
                                }
                            }
                        } else {
                            //another transaction has committed, so we need to do a full readconflict test
                            //todo: seperate between a write conflict and a writeskew (read conflict).
                            if (hasReadWriteConflict()) {
                                throw createWriteSkewConflict();
                            }

                            writeVersion = config.clock.strictTick();
                        }
                    }
                    failure = false;
                } finally {
                    if (failure) {
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

    protected final boolean isDirty(AlphaTranlocal tranlocal) {
        if (tranlocal == null) {
            return false;
        }

        if (tranlocal.isCommitted()) {
            return false;
        }

        //todo: now eager
        if (tranlocal.isCommuting()) {
            return true;
        }

        if (!config.dirtyCheckEnabled) {
            return true;
        }

        return tranlocal.executeDirtyCheck();
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
    protected abstract boolean hasReadWriteConflict();

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
    protected abstract boolean tryWriteLocks(CommitLockFilter commitLockFilter);

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

        if (tranlocal.___writeVersion != writeVersion) {
            release = false;
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

        tranlocal.lateFixation(this);

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
