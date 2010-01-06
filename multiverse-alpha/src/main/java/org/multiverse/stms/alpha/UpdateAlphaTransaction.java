package org.multiverse.stms.alpha;

import org.multiverse.api.exceptions.*;
import org.multiverse.stms.AbstractTransaction;
import static org.multiverse.stms.alpha.AlphaStmUtils.toAtomicObjectString;
import org.multiverse.utils.Listeners;
import static org.multiverse.utils.commitlock.CommitLockUtils.nothingToLock;
import static org.multiverse.utils.commitlock.CommitLockUtils.releaseLocks;
import org.multiverse.utils.latches.Latch;

import static java.lang.String.format;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A {@link org.multiverse.api.Transaction} implementation that is used to do updates. It can also be used for reaonly
 * transaction, but a {@link ReadonlyAlphaTransaction} would be a better candidate for that.
 * <p/>
 * Comment about design: A state design pattern would have been a solution to reduce the switch statements, but to
 * prevent object creation, this is not done.
 *
 * @author Peter Veentjer.
 */
public class UpdateAlphaTransaction extends AbstractTransaction<UpdateTransactionDependencies>
        implements AlphaTransaction {

    private final static AlphaTranlocal[] EMPTY_WRITESET = new AlphaTranlocal[0];

    //the attached set contains the Translocals loaded and attached.
    private final Map<AlphaAtomicObject, AlphaTranlocal> attached
            = new IdentityHashMap<AlphaAtomicObject, AlphaTranlocal>(2);

    private SnapshotStack snapshotStack;

    public UpdateAlphaTransaction(UpdateTransactionDependencies params, String familyName) {
        super(params, familyName);
        init();
    }

    protected void doInit() {
        this.snapshotStack = null;
        this.attached.clear();

        if (dependencies.profiler != null) {
            dependencies.profiler.incCounter("updatetransaction.started.count", getFamilyName());
        }
    }

    @Override
    public AlphaTranlocal load(AlphaAtomicObject atomicObject) {
        switch (getStatus()) {
            case active:
                if (atomicObject == null) {
                    return null;
                }

                AlphaTranlocal tranlocal = attached.get(atomicObject);
                if (tranlocal == null) {
                    try {
                        tranlocal = atomicObject.___loadUpdatable(getReadVersion());
                    } catch (LoadTooOldVersionException e) {
                        if (dependencies.profiler != null) {
                            dependencies.profiler.incCounter("atomicobject.snapshottooold.count",
                                                             atomicObject.getClass().getName());
                            dependencies.profiler.incCounter("updatetransaction.snapshottooold.count", getFamilyName());
                        }
                        throw e;
                    } catch (LoadLockedException e) {
                        if (dependencies.profiler != null) {
                            dependencies.profiler.incCounter("atomicobject.lockedload.count",
                                                             atomicObject.getClass().getName());
                            dependencies.profiler.incCounter("updatetransaction.failedtolock.count", getFamilyName());
                        }
                        throw e;
                    }

                    attached.put(atomicObject, tranlocal);

                    if (dependencies.profiler != null) {
                        dependencies.profiler.incCounter("atomicobject.load.count", atomicObject.getClass().getName());
                        dependencies.profiler.incCounter("updatetransaction.load.count", getFamilyName());
                    }
                } else {
                    if (dependencies.profiler != null) {
                        dependencies.profiler.incCounter("atomicobject.uselessload.count",
                                                         atomicObject.getClass().getName());
                        dependencies.profiler.incCounter("updatetransaction.uselessload.count", getFamilyName());
                    }
                }

                return tranlocal;
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

        attached.clear();
        return commitVersion;
    }

    private long doCommit() {
        AlphaTranlocal[] writeSet = createWriteSet();
        if (nothingToLock(writeSet)) {
            //if there is nothing to commit, we are done.
            if (dependencies.profiler != null) {
                dependencies.profiler.incCounter("updatetransaction.emptycommit.count", getFamilyName());
            }
            return getReadVersion();
        }

        boolean locksNeedToBeReleased = true;
        long writeVersion = 0;
        try {
            acquireLocksAndCheckForConflicts(writeSet);
            writeVersion = dependencies.clock.tick();

            if (SANITY_CHECKS_ENABLED) {
                if (writeVersion <= getReadVersion()) {
                    throw new PanicError("The clock went back in time");
                }
            }

            storeAllAndReleaseLocks(writeSet, writeVersion);
            locksNeedToBeReleased = true;
            return writeVersion;
        } finally {
            if (locksNeedToBeReleased) {
                releaseLocks(writeSet, this);
            }
        }
    }

    /**
     * Creates the writeset; a set of objects which state needs to be committed.
     *
     * @return the created WriteSet. The returned value will never be null.
     *
     * @throws org.multiverse.api.exceptions.WriteConflictException
     *          if can be determined that another transaction did a conflicting write.
     */
    private AlphaTranlocal[] createWriteSet() {
        if (attached.isEmpty()) {
            return EMPTY_WRITESET;
        }

        AlphaTranlocal[] writeSet = null;

        int skipped = 0;
        int index = 0;
        for (AlphaTranlocal tranlocal : attached.values()) {
            switch (tranlocal.getDirtinessStatus()) {
                case clean:
                    //fall through
                case readonly:
                    skipped++;
                    break;
                case fresh:
                    //fall through
                case dirty:
                    if (dependencies.profiler != null) {
                        dependencies.profiler.incCounter(
                                "atomicobject.dirty.count", tranlocal.getAtomicObject().getClass().getName());
                    }

                    if (writeSet == null) {
                        writeSet = new AlphaTranlocal[attached.size() - skipped];
                    }
                    writeSet[index] = tranlocal;
                    index++;
                    break;
                case conflict:
                    //if we can already determine that the write can never happen, start a write conflict
                    //and fail immediately.
                    if (dependencies.profiler != null) {
                        dependencies.profiler.incCounter("atomicobject.conflict.count",
                                                         tranlocal.getAtomicObject().getClass().getName());
                        dependencies.profiler.incCounter("updatetransaction.writeconflict.count", getFamilyName());
                    }

                    if (WriteConflictException.reuse) {
                        throw WriteConflictException.INSTANCE;
                    } else {
                        String msg = format(
                                "There was a writeconflict in transaction with familyname '%s' on atomicobject '%s'",
                                getFamilyName(),
                                toAtomicObjectString(tranlocal));
                        throw new WriteConflictException(msg);
                    }
                default:
                    throw new RuntimeException();
            }
        }

        return writeSet == null ? EMPTY_WRITESET : writeSet;
    }

    private void acquireLocksAndCheckForConflicts(AlphaTranlocal[] writeSet) {
        switch (dependencies.commitLockPolicy.tryLockAllAndDetectConflicts(writeSet, this)) {
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

    private void storeAllAndReleaseLocks(AlphaTranlocal[] writeSet, long commitVersion) {
        try {
            for (int k = 0; k < writeSet.length; k++) {
                AlphaTranlocal tranlocal = writeSet[k];
                if (tranlocal == null) {
                    return;
                } else {
                    AlphaAtomicObject atomicObject = tranlocal.getAtomicObject();
                    Listeners listeners = atomicObject.___storeAndReleaseLock(tranlocal, commitVersion);
                    if (listeners != null) {
                        listeners.openAll();
                    }
                }
            }
        } finally {
            if (dependencies.profiler != null) {
                dependencies.profiler.incCounter("updatetransaction.individualwrite.count",
                                                 getFamilyName(),
                                                 attached.size());
            }
        }
    }

    @Override
    protected void doAbort() {
        attached.clear();
        if (dependencies.profiler != null) {
            dependencies.profiler.incCounter("updatetransaction.aborted.count", getFamilyName());
        }
    }

    @Override
    protected void doAbortAndRegisterRetryLatch(Latch latch) {
        if (attached.isEmpty()) {
            String msg = format("Can't retry on transaction '%s' because it has not been used.", getFamilyName());
            throw new NoRetryPossibleException(msg);
        }

        if (dependencies.profiler != null) {
            dependencies.profiler.incCounter("updatetransaction.waiting.count", getFamilyName());
        }

        long minimalVersion = getReadVersion() + 1;

        boolean atLeastOneRegistration = false;
        for (AlphaAtomicObject atomicObject : attached.keySet()) {
            if (atomicObject.___registerRetryListener(latch, minimalVersion)) {
                atLeastOneRegistration = true;

                if (latch.isOpen()) {
                    break;
                }
            }
        }

        if (!atLeastOneRegistration) {
            String msg = format("Can't retry on transaction '%s' because it has no reads", getFamilyName());
            throw new NoRetryPossibleException(msg);
        }
    }

    @Override
    protected void doStartOr() {
        snapshotStack = new SnapshotStack(snapshotStack, createSnapshot());
    }

    private AlphaTranlocalSnapshot createSnapshot() {
        AlphaTranlocalSnapshot result = null;
        for (AlphaTranlocal tranlocal : attached.values()) {
            AlphaTranlocalSnapshot snapshot = tranlocal.takeSnapshot();
            snapshot.___next = result;
            result = snapshot;
        }

        return result;
    }

    @Override
    protected void doEndOr() {
        if (snapshotStack == null) {
            throw new IllegalStateException();
        }
        snapshotStack = snapshotStack.next;
    }

    @Override
    protected void doEndOrAndStartElse() {
        if (snapshotStack == null) {
            throw new IllegalStateException();
        }
        AlphaTranlocalSnapshot snapshot = snapshotStack.snapshot;
        snapshotStack = snapshotStack.next;
        restoreSnapshot(snapshot);
    }

    private void restoreSnapshot(AlphaTranlocalSnapshot snapshot) {
        attached.clear();

        while (snapshot != null) {
            AlphaTranlocal tranlocal = snapshot.getTranlocal();
            attached.put(tranlocal.getAtomicObject(), tranlocal);
            snapshot.restore();
            snapshot = snapshot.___next;
        }
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