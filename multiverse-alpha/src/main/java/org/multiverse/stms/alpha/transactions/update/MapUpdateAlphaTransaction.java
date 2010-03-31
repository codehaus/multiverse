package org.multiverse.stms.alpha.transactions.update;

import org.multiverse.api.Latch;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.UncommittedFilter;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.utils.Listeners;
import org.multiverse.utils.commitlock.CommitLock;

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
public class MapUpdateAlphaTransaction extends AbstractUpdateAlphaTransaction {

    public static class Factory implements TransactionFactory<AlphaTransaction> {

        public final UpdateAlphaTransactionConfiguration config;

        public Factory(UpdateAlphaTransactionConfiguration config) {
            this.config = config;
        }

        @Override
        public AlphaTransaction start() {
            return new MapUpdateAlphaTransaction(config);
        }
    }

    private final Map<AlphaTransactionalObject, AlphaTranlocal> attachedMap =
            new IdentityHashMap<AlphaTransactionalObject, AlphaTranlocal>(30);

    public MapUpdateAlphaTransaction(UpdateAlphaTransactionConfiguration config) {
        super(config);
        init();
        //todo: the size of the attachedMap could be chosen a littlebit better
    }

    @Override
    protected void doClear() {
        attachedMap.clear();
    }

    @Override
    protected void attach(AlphaTranlocal tranlocal) {
        attachedMap.put(tranlocal.getTransactionalObject(), tranlocal);
    }

    @Override
    protected AlphaTranlocal findAttached(AlphaTransactionalObject txObject) {
        return attachedMap.get(txObject);
    }

    @Override
    protected boolean tryWriteLocks() {
        return config.commitLockPolicy.tryAcquireAll(
                (Collection<CommitLock>) ((Object) attachedMap.values()),
                config.dirtyCheck ? UncommittedFilter.DIRTY_CHECK : UncommittedFilter.NO_DIRTY_CHECK,
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
    protected boolean hasWrites() {
        boolean isDirty = false;

        for (AlphaTranlocal attached : attachedMap.values()) {
            if (hasWrite(attached)) {
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
    protected boolean hasReadConflict() {
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
    protected AlphaTranlocal doOpenForCommutingWrite(AlphaTransactionalObject txObject) {
        AlphaTranlocal attached = attachedMap.get(txObject);


        if(attached == null){
            attached = txObject.___openForCommutingOperation();
            attachedMap.put(txObject, attached);
        }else if(attached.isCommitted()){
            attached = attached.openForWrite();
            attachedMap.put(txObject, attached);
        }

        return attached;
    }

    @Override
    protected AlphaTranlocal doOpenForWrite(AlphaTransactionalObject txObject) {
        AlphaTranlocal attached = attachedMap.get(txObject);
        if (attached == null) {
            //it isnt loaded before.
            AlphaTranlocal committed = txObject.___load(getReadVersion());
            if (committed == null) {
                attached = txObject.___openUnconstructed();
            } else {
                attached = committed.openForWrite();
            }

            attachedMap.put(txObject, attached);
        } else if (attached.isCommitted()) {
            //it is loaded before but it is a readonly
            //make an updatable clone of the tranlocal already is committed and use that
            //from now on.
            attached = attached.openForWrite();
            attachedMap.put(txObject, attached);
        } else if (attached.isCommuting()) {
            AlphaTranlocal origin = txObject.___load(getReadVersion());
            if(origin == null){
                throw new UncommittedReadConflict();
            }

            attached.fixatePremature(this, origin);
        }

        return attached;
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