package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.Latch;
import org.multiverse.api.exceptions.FailedToObtainCommitLocksException;
import org.multiverse.api.exceptions.WriteConflictException;
import org.multiverse.stms.AbstractTransactionSnapshot;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AbstractAlphaTransaction;
import org.multiverse.utils.Listeners;

import static java.lang.String.format;

/**
 * An abstract {@link org.multiverse.stms.alpha.transactions.AlphaTransaction} that provides all the pluming logic for
 * update transactions.
 *
 * @author Peter Veentjer.
 */
public abstract class AbstractUpdateAlphaTransaction<C extends AbstractUpdateAlphaTransactionConfig>
        extends AbstractAlphaTransaction<C, AbstractTransactionSnapshot> {

    private long writeVersion;

    public AbstractUpdateAlphaTransaction(C config) {
        super(config);
    }

    @Override
    protected final boolean doRegisterRetryLatch(Latch latch, long wakeupVersion) {
        if (!config.automaticReadTracking) {
            return false;
        }

        return dodoRegisterRetryLatch(latch, wakeupVersion);
    }

    protected abstract boolean dodoRegisterRetryLatch(Latch latch, long wakeupVersion);

    @Override
    protected final AlphaTranlocal doOpenForRead(AlphaTransactionalObject txObject) {
        AlphaTranlocal attached = find(txObject);
        if (attached != null) {
            return attached;
        }

        attached = txObject.___load(getReadVersion());
        if (attached == null) {
            attached = txObject.___openUnconstructed();
            attach(attached);
        } else if (config.automaticReadTracking) {
            attach(attached);
        }

        return attached;
    }

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
    protected abstract AlphaTranlocal find(AlphaTransactionalObject txObject);

    /**
     * Returns the state of the attached tranlocals. This information is needed for the transaction to decide what to do
     * (write changes etc). It is important that the implementation should not stop after it finds one element that is
     * dirty, it should check all attached objects. This is because the isDirty flag on each tranlocal needs to be
     * cached for later use.
     *
     * @return the AttachedState.
     */
    protected abstract boolean hasWrites();

    protected final boolean hasWrite(AlphaTranlocal attached) {
        if (attached == null) {
            return false;
        }

        if (attached.isCommitted()) {
            return false;
        }

        if (config.dirtyCheck) {
            return attached.isDirtySweep();
        } else {
            return true;
        }
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
        } else if (config.dirtyCheck && !tranlocal.getPrecalculatedIsDirty()) {
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
     */
    protected abstract Listeners[] store(long writeVersion);

    protected final Listeners store(AlphaTranlocal tranlocal, long writeVersion) {
        if (tranlocal == null) {
            return null;
        }

        AlphaTransactionalObject txObject = tranlocal.getTransactionalObject();

        if (tranlocal.isCommitted()) {
            return null;
        }

        if (config.dirtyCheck) {
            if (tranlocal.getPrecalculatedIsDirty()) {
                return txObject.___store(tranlocal, writeVersion);
            } else {
                return null;
            }
        } else {
            return txObject.___store(tranlocal, writeVersion);
        }
    }

    @Override
    protected final void doPrepare() {
        if (!hasWrites()) {
            return;
        }

        if (!tryWriteLocks()) {
            throw createFailedToObtainCommitLocksException();
        }

        boolean hasConflict;
        if (config.optimizedConflictDetection && getReadVersion() == config.clock.getVersion()) {
            writeVersion = config.clock.tick();
            //it could be that a different transaction also reached this part, so we need to make sure
            hasConflict = writeVersion != getReadVersion() + 1;
        } else if (config.preventWriteSkew) {
            writeVersion = config.clock.tick();
            hasConflict = hasReadConflict();
        } else {
            hasConflict = hasWriteConflict();
            if (!hasConflict) {
                writeVersion = config.clock.tick();
            }
        }

        if (hasConflict) {
            doReleaseWriteLocksForFailure();
            throw createWriteConflictException();
        }
    }

    @Override
    protected void doAbortPrepared() {
        doReleaseWriteLocksForFailure();
    }

    @Override
    protected void doCommitPrepared() {
        Listeners[] listeners = store(writeVersion);
        doReleaseWriteLocksForSuccess(writeVersion);
        openAll(listeners);
    }

    private WriteConflictException createWriteConflictException() {
        if (WriteConflictException.reuse) {
            return WriteConflictException.INSTANCE;
        }

        String msg = format(
                "Failed to commit transaction '%s' because there was a write conflict'", config.getFamilyName());
        return new WriteConflictException(msg);
    }

    private FailedToObtainCommitLocksException createFailedToObtainCommitLocksException() {
        if (FailedToObtainCommitLocksException.reuse) {
            return FailedToObtainCommitLocksException.INSTANCE;
        }

        String msg = format(
                "Failed to commit transaction '%s' because not all the locks on the transactional objects " +
                        "in the writeset could be obtained",
                config.getFamilyName());
        return new FailedToObtainCommitLocksException(msg);
    }

    /**
     * Opens all listeners. Stops as soon as it finds a null, and can safely be called with a null listenersArray.
     *
     * @param arrayOfListeners the array of Listeners.
     */
    private static void openAll(Listeners[] arrayOfListeners) {
        if (arrayOfListeners == null) {
            return;
        }

        for (int k = 0; k < arrayOfListeners.length; k++) {
            Listeners listeners = arrayOfListeners[k];
            if (listeners == null) {
                return;
            }
            listeners.openAll();
        }
    }
}
