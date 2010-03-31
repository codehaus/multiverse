package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.Latch;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.UncommittedFilter;
import org.multiverse.utils.Listeners;
import org.multiverse.utils.commitlock.CommitLockPolicy;

/**
 * A AbstractUpdateAlphaTransaction that can deal with a single transactional object.
 *
 * @author Peter Veentjer
 */
public class MonoUpdateAlphaTransaction extends AbstractUpdateAlphaTransaction {

    private AlphaTranlocal attached;

    public MonoUpdateAlphaTransaction(UpdateAlphaTransactionConfiguration config) {
        super(config);

        init();
    }

    @Override
    protected void doClear() {
        attached = null;
    }

    @Override
    protected boolean tryWriteLocks() {
        CommitLockPolicy lockPolicy = config.commitLockPolicy;
        return lockPolicy.tryAcquire(
                attached,
                config.dirtyCheck ? UncommittedFilter.DIRTY_CHECK : UncommittedFilter.NO_DIRTY_CHECK,
                this);
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
    protected boolean hasWrites() {
        return hasWrite(attached);
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
            config.speculativeConfiguration.signalSpeculativeSizeFailure(1);
            throw SpeculativeConfigurationFailure.create();
        }

        attached = tranlocal;
    }

    @Override
    protected AlphaTranlocal findAttached(AlphaTransactionalObject txObject) {
        return attached == null || attached.getTransactionalObject() != txObject ? null : attached;
    }

    @Override
    protected AlphaTranlocal doOpenForCommutingWrite(AlphaTransactionalObject txObject) {
        if (attached == null) {
            attached = txObject.___openForCommutingOperation();
        } else if (attached.getTransactionalObject() == txObject) {
            if(attached.isCommitted()){
                attached = attached.openForWrite();
            }
        } else {
            config.speculativeConfiguration.signalSpeculativeSizeFailure(1);
            throw SpeculativeConfigurationFailure.create();
        }

        return attached;
    }

    @Override
    protected AlphaTranlocal doOpenForWrite(AlphaTransactionalObject txObject) {
        if (attached != null) {
            if (attached.getTransactionalObject() != txObject) {
                config.speculativeConfiguration.signalSpeculativeSizeFailure(1);
                throw SpeculativeConfigurationFailure.create();
            }

            if (attached.isCommitted()) {
                attached = attached.openForWrite();
            } else if (attached.isCommuting()) {
                AlphaTranlocal origin = txObject.___load(getReadVersion());
                if (origin == null) {
                    throw new UncommittedReadConflict();
                }
                attached.fixatePremature(this, origin);
            }
        } else {
            AlphaTranlocal committed = txObject.___load(getReadVersion());
            if (committed == null) {
                attached = txObject.___openUnconstructed();
            } else {
                attached = committed.openForWrite();
            }
        }

        return attached;

    }

    @Override
    protected boolean dodoRegisterRetryLatch(Latch latch, long wakeupVersion) {
        if (attached == null) {
            return false;
        }

        AlphaTransactionalObject txObject = attached.getTransactionalObject();
        switch (txObject.___registerRetryListener(latch, wakeupVersion)) {
            case registered:
            case opened:
                return true;
            case noregistration:
                return false;
            default:
                throw new IllegalStateException();
        }
    }
}