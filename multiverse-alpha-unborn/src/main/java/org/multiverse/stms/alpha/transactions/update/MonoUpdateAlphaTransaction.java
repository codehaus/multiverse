package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.Listeners;
import org.multiverse.api.commitlock.CommitLockFilter;
import org.multiverse.api.commitlock.CommitLockPolicy;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

/**
 * A AbstractUpdateAlphaTransaction that can deal with a single transactional object.
 *
 * @author Peter Veentjer
 */
public final class MonoUpdateAlphaTransaction extends AbstractUpdateAlphaTransaction {

    private AlphaTranlocal attached;

    public MonoUpdateAlphaTransaction(UpdateConfiguration config) {
        super(config);
        init();
    }

    @Override
    protected void dodoClear() {
        attached = null;
    }

    @Override
    protected boolean tryWriteLocks(CommitLockFilter commitLockFilter) {
        CommitLockPolicy lockPolicy = config.commitLockPolicy;
        return lockPolicy.tryAcquire(attached, commitLockFilter, this);
    }

    @Override
    protected void doReleaseWriteLocksForFailure() {
        doReleaseWriteSetLocksForFailure(attached);
    }

    @Override
    protected void doReleaseWriteLocksForSuccess(long writeVersion) {
        doReleaseWriteLockForSuccess(attached, writeVersion);
    }

    @Override
    protected boolean isDirty() {
        return isDirty(attached);
    }

    @Override
    protected boolean hasWriteConflict() {
        return hasWriteConflict(attached);
    }

    @Override
    protected boolean hasReadConflict() {
        return hasWriteConflict(attached);
    }

    @Override
    protected Listeners[] makeChangesPermanent(long writeVersion) {
        Listeners listeners = makePermanent(attached, writeVersion);
        return listeners == null ? null : new Listeners[]{listeners};
    }

    @Override
    protected void attach(AlphaTranlocal tranlocal) {
        if (attached != null) {
            if (attached.getTransactionalObject() != tranlocal.getTransactionalObject()) {
                config.speculativeConfiguration.signalSpeculativeSizeFailure(1);
                throw SpeculativeConfigurationFailure.create();
            }

            attached = tranlocal;
        }

        attached = tranlocal;
    }

    @Override
    protected AlphaTranlocal findAttached(AlphaTransactionalObject txObject) {
        if (attached == null || attached.getTransactionalObject() != txObject) {
            return null;
        } else {
            return attached;
        }
    }

    @Override
    protected boolean dodoRegisterRetryLatch(Latch latch, long wakeupVersion) {
        if (attached == null) {
            return false;
        }

        AlphaTransactionalObject txObject = attached.getTransactionalObject();
        switch (txObject.___registerRetryListener(latch, wakeupVersion)) {
            case registered:
                //fall through
            case opened:
                return true;
            case noregistration:
                return false;
            default:
                throw new IllegalStateException();
        }
    }
}