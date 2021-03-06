package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.Listeners;
import org.multiverse.api.commitlock.CommitLock;
import org.multiverse.api.commitlock.CommitLockFilter;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A {@link AbstractUpdateAlphaTransaction} implementation where the AlphaTranlocals are stored in an
 * IdentityHashMap (to prevent unwanted equals/hashcode calls on AlphaTransactionalObjects.
 * <p/>
 * This implementation is good for larger transaction sizes and is able to grow. In the future certain parts of this
 * implementation (like writing, conflict detection etc) could be executed in parallel (e.g. by using the fork join
 * framework).
 *
 * @author Peter Veentjer.
 */
public final class MapUpdateAlphaTransaction extends AbstractUpdateAlphaTransaction {

    private final Map<AlphaTransactionalObject, AlphaTranlocal> attachedMap =
            new IdentityHashMap<AlphaTransactionalObject, AlphaTranlocal>(30);

    public MapUpdateAlphaTransaction(UpdateConfiguration config) {
        super(config);
        //todo: the size of the attachedMap could be chosen a littlebit better
    }

    @Override
    protected void doDoReset() {
        attachedMap.clear();
    }

    @Override
    protected void attach(AlphaTranlocal tranlocal) {
        attachedMap.put(tranlocal.getTransactionalObject(), tranlocal);
    }

    @Override
    protected AlphaTranlocal findAttached(AlphaTransactionalObject transactionalObject) {
        return attachedMap.get(transactionalObject);
    }

    @Override
    protected boolean tryWriteLocks(CommitLockFilter commitLockFilter) {
        return config.commitLockPolicy.tryAcquireAll(
                (Collection<CommitLock>) ((Object) attachedMap.values()),
                commitLockFilter,
                this);
    }

    @Override
    protected void doReleaseWriteLocksForFailure() {
        for (AlphaTranlocal tranlocal : attachedMap.values()) {
            doReleaseWriteSetLocksForFailure(tranlocal);
        }
    }

    @Override
    protected void doReleaseWriteLocksForSuccess(long writeVersion) {
        for (AlphaTranlocal tranlocal : attachedMap.values()) {
            doReleaseWriteLockForSuccess(tranlocal, writeVersion);
        }
    }

    @Override
    protected boolean isDirty() {
        boolean isDirty = false;

        for (AlphaTranlocal attached : attachedMap.values()) {
            if (isDirty(attached)) {
                isDirty = true;
            }
        }
        return isDirty;
    }

    @Override
    protected boolean hasWriteConflict() {
        for (AlphaTranlocal attached : attachedMap.values()) {
            if (hasWriteConflict(attached)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean hasReadWriteConflict() {
        for (AlphaTranlocal attached : attachedMap.values()) {
            if (hasReadConflict(attached)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected Listeners[] makeChangesPermanent(long writeVersion) {
        Listeners[] listenersArray = null;

        int index = 0;
        int listenersIndex = 0;
        for (AlphaTranlocal attached : attachedMap.values()) {
            Listeners listeners = makePermanent(attached, writeVersion);

            if (listeners != null) {
                if (listenersArray == null) {
                    listenersArray = new Listeners[attachedMap.size() - index];
                }

                listenersArray[listenersIndex] = listeners;
                listenersIndex++;
            }
            index++;
        }

        return listenersArray;
    }

    @Override
    protected boolean dodoRegisterRetryLatch(Latch latch, long wakeupVersion) {
        if (attachedMap.isEmpty()) {
            return false;
        }

        boolean trackedReads = false;

        for (AlphaTransactionalObject txObject : attachedMap.keySet()) {
            switch (txObject.___registerRetryListener(latch, wakeupVersion)) {
                case noregistration:
                    break;
                case registered:
                    trackedReads = true;
                    break;
                case opened:
                    return true;
                default:
                    throw new IllegalStateException();
            }
        }

        return trackedReads;
    }
}