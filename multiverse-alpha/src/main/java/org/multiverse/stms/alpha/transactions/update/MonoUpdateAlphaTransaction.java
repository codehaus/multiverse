package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.exceptions.TransactionTooSmallException;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.UncommittedFilter;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.Listeners;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.latches.Latch;
import org.multiverse.utils.profiling.ProfileRepository;

/**
 * A AbstractUpdateAlphaTransaction that can deal with a single transactional object.
 *
 * @author Peter Veentjer
 */
public class MonoUpdateAlphaTransaction
        extends AbstractUpdateAlphaTransaction<MonoUpdateAlphaTransaction.Config> {

    public static class Config extends AbstractUpdateAlphaTransactionConfig {

        public final OptimalSize optimalSize;

        public Config(
                PrimitiveClock clock, BackoffPolicy backoffPolicy, String familyName, ProfileRepository profiler,
                int maxRetryCount, CommitLockPolicy commitLockPolicy, boolean interruptible, OptimalSize optimalSize,
                boolean allowWriteSkewProblem, boolean optimizeConflictDetection, boolean dirtyCheck,
                boolean automaticReadTracking) {
            super(clock, backoffPolicy, familyName, false, maxRetryCount, interruptible, commitLockPolicy,
                    profiler, allowWriteSkewProblem, automaticReadTracking, optimizeConflictDetection, dirtyCheck);

            this.optimalSize = optimalSize;
        }
    }

    private AlphaTranlocal attached;

    public MonoUpdateAlphaTransaction(Config config) {
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
    protected Listeners[] store(long writeVersion) {
        Listeners listeners = store(attached, writeVersion);
        return listeners == null ? null : new Listeners[]{listeners};
    }

    @Override
    protected void attach(AlphaTranlocal tranlocal) {
        if (attached != null) {
            config.optimalSize.compareAndSet(1, 2);
            throw TransactionTooSmallException.INSTANCE;
        }

        attached = tranlocal;
    }

    @Override
    protected AlphaTranlocal find(AlphaTransactionalObject txObject) {
        return attached == null || attached.getTransactionalObject() != txObject ? null : attached;
    }

    @Override
    protected AlphaTranlocal doOpenForWrite(AlphaTransactionalObject txObject) {
        if (attached != null) {
            if (attached.getTransactionalObject() != txObject) {
                config.optimalSize.compareAndSet(1, 2);
                throw TransactionTooSmallException.INSTANCE;
            }

            if (attached.isCommitted()) {
                attached = attached.openForWrite();
            } else if (attached.isUnfixated()) {
                attached.fixate(this);
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