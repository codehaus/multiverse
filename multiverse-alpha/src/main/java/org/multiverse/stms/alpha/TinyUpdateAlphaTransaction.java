package org.multiverse.stms.alpha;

import org.multiverse.api.exceptions.*;
import org.multiverse.stms.AbstractTransaction;
import static org.multiverse.stms.alpha.AlphaStmUtils.toAtomicObjectString;
import org.multiverse.utils.Listeners;
import org.multiverse.utils.TodoException;
import org.multiverse.utils.latches.Latch;

import static java.lang.String.format;

/**
 * @author Peter Veentjer
 */
public class TinyUpdateAlphaTransaction extends AbstractTransaction<UpdateTransactionDependencies>
        implements AlphaTransaction {

    private final static AlphaTranlocal[] EMPTY_WRITESET = new AlphaTranlocal[0];

    private AlphaTranlocal attached;

    public TinyUpdateAlphaTransaction(UpdateTransactionDependencies params, String familyName) {
        super(params, familyName);
        init();
    }

    protected void doInit() {
        this.attached = null;
    }

    @Override
    public AlphaTranlocal load(AlphaAtomicObject atomicObject) {
        switch (getStatus()) {
            case active:
                if (atomicObject == null) {
                    return null;
                }

                if (attached != null) {
                    if (attached.getAtomicObject() != atomicObject) {
                        throw new RuntimeException();
                    }

                    return attached;
                } else {
                    attached = atomicObject.___loadUpdatable(getReadVersion());
                    return attached;
                }
            case committed: {
                String msg = format("Can't call load with atomicobject '%s' on committed transaction '%s'.",
                                    toAtomicObjectString(atomicObject), familyName);
                throw new DeadTransactionException(msg);
            }
            case aborted: {
                String msg = format("Can't call load with atomicObject '%s' on aborted transaction '%s'.",
                                    toAtomicObjectString(atomicObject), familyName);
                throw new DeadTransactionException(msg);
            }
            default:
                throw new RuntimeException();
        }
    }

    @Override
    protected long onCommit() {
        long commitVersion = doCommit();
        if (dependencies.profiler != null) {
            dependencies.profiler.incCounter("updatetransaction.committed.count", getFamilyName());
        }

        attached = null;
        return commitVersion;
    }

    private long doCommit() {
        if (attached == null) {
            return getReadVersion();
        }

        switch (attached.getDirtinessStatus()) {
            case clean:
                return getReadVersion();
            case readonly:
                return getReadVersion();
            case fresh:
                //fall through
            case dirty:
                break;
            case conflict:
                if (WriteConflictException.reuse) {
                    throw WriteConflictException.INSTANCE;
                } else {
                    String msg = format(
                            "There was a writeconflict in transaction with familyname '%s' on atomicobject '%s'",
                            getFamilyName(),
                            toAtomicObjectString(attached));
                    throw new WriteConflictException(msg);
                }
            default:
                throw new RuntimeException();
        }

        boolean locksNeedToBeReleased = true;
        try {
            acquireLocksAndCheckForConflicts();
            long writeVersion = dependencies.clock.tick();

            if (SANITY_CHECKS_ENABLED) {
                if (writeVersion <= getReadVersion()) {
                    throw new PanicError("The clock went back in time");
                }
            }

            storeAllAndReleaseLocks(writeVersion);
            locksNeedToBeReleased = false;
            return writeVersion;
        } finally {
            if (locksNeedToBeReleased) {
                attached.getAtomicObject().___releaseLock(this);
            }
        }
    }


    private void acquireLocksAndCheckForConflicts() {
        switch (dependencies.commitLockPolicy.tryLockAndDetectConflict(attached, this)) {
            case success:
                //todo: problem is that if the locks are not acquired successfully, it isn't clear
                //how many locks were acquired.
                if (dependencies.profiler != null) {
                    dependencies.profiler.incCounter("updatetransaction.acquirelocks.count", getFamilyName());
                }
                break;
            case failure:
                if (dependencies.profiler != null) {
                    dependencies.profiler.incCounter("updatetransaction.failedtoacquirelocks.count", getFamilyName());
                }

                if (FailedToObtainLocksException.reuse) {
                    throw FailedToObtainLocksException.INSTANCE;
                } else {
                    String msg = format(
                            "Failed to obtain all locks needed for commit on transaction wuth familyname '%s'",
                            getFamilyName());
                    throw new FailedToObtainLocksException(msg);
                }
            case conflict:
                if (dependencies.profiler != null) {
                    dependencies.profiler.incCounter("updatetransaction.writeconflict.count", getFamilyName());
                }
                if (WriteConflictException.reuse) {
                    throw WriteConflictException.INSTANCE;
                } else {
                    String msg = format("There was a writeconflict in transaction with familyname '%s'",
                                        getFamilyName());
                    throw new WriteConflictException(msg);
                }

            default:
                throw new RuntimeException();
        }
    }

    private void storeAllAndReleaseLocks(long commitVersion) {
        AlphaAtomicObject atomicObject = attached.getAtomicObject();
        Listeners listeners = atomicObject.___storeAndReleaseLock(attached, commitVersion);
        if (listeners != null) {
            listeners.openAll();
        }
    }

    @Override
    protected void doAbort() {
        attached = null;
    }

    @Override
    protected void doAbortAndRegisterRetryLatch(Latch latch) {
        if (attached == null) {
            String msg = format("Can't retry on transaction '%s' because it has not been used.", getFamilyName());
            throw new NoRetryPossibleException(msg);
        }

        if (dependencies.profiler != null) {
            dependencies.profiler.incCounter("updatetransaction.waiting.count", getFamilyName());
        }

        long minimalVersion = getReadVersion() + 1;

        boolean atLeastOneRegistration = false;
        if (attached.getAtomicObject().___registerRetryListener(latch, minimalVersion)) {
            atLeastOneRegistration = true;
        }

        if (!atLeastOneRegistration) {
            String msg = format("Can't retry on transaction '%s' because it has no reads", getFamilyName());
            throw new NoRetryPossibleException(msg);
        }
    }

    @Override
    protected void doStartOr() {
        throw new TodoException();
    }

    private AlphaTranlocalSnapshot createSnapshot() {
        throw new TodoException();
    }

    @Override
    protected void doEndOr() {
        throw new TodoException();
    }

    @Override
    protected void doEndOrAndStartElse() {
        throw new TodoException();
    }

    private void restoreSnapshot(AlphaTranlocalSnapshot snapshot) {
        throw new TodoException();
    }

    static final class SnapshotStack {

        public final SnapshotStack next;
        public final AlphaTranlocalSnapshot snapshot;

        SnapshotStack(SnapshotStack next, AlphaTranlocalSnapshot snapshot) {
            this.next = next;
            this.snapshot = snapshot;
        }
    }
}