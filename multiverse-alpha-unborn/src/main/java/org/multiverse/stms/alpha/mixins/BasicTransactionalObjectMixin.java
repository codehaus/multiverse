package org.multiverse.stms.alpha.mixins;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.Listeners;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockNotFreeReadConflict;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaStmUtils;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.RegisterRetryListenerResult;
import org.multiverse.utils.TodoException;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static java.lang.String.format;

/**
 * AlphaTransactionalObject implementation that also can be used to transplant methods from during instrumentation. This
 * implementation supports listeners.
 * <p/>
 * It is important that the constructor doesn't contain any donorMethod because the constructor code is not copied when
 * this class is 'mixed' in. In the future perhaps this is fixed when there needs to be a constructor. So you are
 * warned.
 *
 * @author Peter Veentjer
 */
public abstract class BasicTransactionalObjectMixin implements AlphaTransactionalObject, MultiverseConstants {

    private final static AtomicReferenceFieldUpdater<BasicTransactionalObjectMixin, Transaction> ___LOCKOWNER_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(BasicTransactionalObjectMixin.class, Transaction.class, "___lockOwner");

    private final static AtomicReferenceFieldUpdater<BasicTransactionalObjectMixin, Listeners> ___LISTENERS_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(BasicTransactionalObjectMixin.class, Listeners.class, "___listeners");

    private volatile Transaction ___lockOwner;
    private volatile AlphaTranlocal ___tranlocal;
    private volatile Listeners ___listeners;

    // ==================== loading ===============================

    @Override
    public final AlphaTranlocal ___load() {
        return ___tranlocal;
    }

    @Override
    public final AlphaTranlocal ___load(long readVersion) {
        AlphaTranlocal tranlocalTime1 = ___tranlocal;

        if (tranlocalTime1 == null) {
            //a read is done, but there is no committed data.
            return null;
        }

        if (tranlocalTime1.getWriteVersion() == readVersion) {
            //we are lucky, the tranlocal is exactly the one we are looking for.
            return tranlocalTime1;
        } else if (tranlocalTime1.getWriteVersion() > readVersion) {
            //we are not lucky, the version that is stored, is too new for us.
            throw createOldVersionNotFoundReadConflict(readVersion, tranlocalTime1);
        } else {
            //the exact version is not there, we need to make sure that the one we
            //are going to load is valid (so not locked).
            Transaction lockOwner = ___LOCKOWNER_UPDATER.get(this);

            if (lockOwner != null) {
                throw createLockNotFreeReadConflict();
            }

            AlphaTranlocal tranlocalTime2 = ___tranlocal;
            boolean noConflictingWrites = tranlocalTime2 == tranlocalTime1;
            if (noConflictingWrites) {
                //the tranlocal has not changed and it was unlocked. This means that we read
                //an old version that we can use.
                return tranlocalTime1;
            } else {
                //if the tranlocal has changed, lets check if the new tranlocal has exactly the
                //version we are looking for.
                if (tranlocalTime2.getWriteVersion() == readVersion) {
                    //we are lucky, the newest version is exactly the one we are looking for.
                    return tranlocalTime2;
                }

                //we ran out of luck, the version that currently is stored is not usable. 
                throw createOldVersionNotFoundReadConflict(readVersion, tranlocalTime2);
            }
        }
    }

    private LockNotFreeReadConflict createLockNotFreeReadConflict() {
        if (LockNotFreeReadConflict.reuse) {
            return LockNotFreeReadConflict.INSTANCE;
        } else {
            String msg = format("Failed to load already locked transactionalobject '%s'",
                    AlphaStmUtils.toTxObjectString(this));
            return new LockNotFreeReadConflict(msg);
        }
    }

    private OldVersionNotFoundReadConflict createOldVersionNotFoundReadConflict(long readVersion, AlphaTranlocal tranlocalTime2) {
        if (OldVersionNotFoundReadConflict.reuse) {
            return OldVersionNotFoundReadConflict.INSTANCE;
        } else {
            String msg = format(
                    "Can't load version '%s' transactionalobject '%s', the oldest version found is '%s'",
                    readVersion,
                    AlphaStmUtils.toTxObjectString(this),
                    tranlocalTime2.getWriteVersion());
            return new OldVersionNotFoundReadConflict(msg);
        }
    }

    @Override
    public AlphaTranlocal ___openForCommutingOperation() {
        throw new TodoException();
    }

    // ==================== store ===============================

    @Override
    //todo: make final
    public Listeners ___storeUpdate(AlphaTranlocal update, long writeVersion, boolean releaseLock) {
        assert update != null;

        //it is very important that the tranlocal write is is done before the lock release.
        //it also is very important that the commit and version are set, before the tranlocal write.
        //the tranlocal write also creates a happens before relation between the changes made on the
        //tranlocal, and the read on the tranlocal.
        update.prepareForCommit(writeVersion);

        ___tranlocal = update;

        Listeners listeners = null;
        if (___LISTENERS_UPDATER.get(this) != null) {
            //it could be that listeners are set af
            listeners = ___LISTENERS_UPDATER.getAndSet(this, null);
        }

        if (releaseLock) {
            ___lockOwner = null;
        }

        return listeners;
    }


    @Override
    public void ___storeInitial(AlphaTranlocal tranlocal, long writeVersion) {
        tranlocal.prepareForCommit(writeVersion);
        ___tranlocal = tranlocal;
    }

    // ===================== locking ==============================================

    @Override
    public final Transaction ___getLockOwner() {
        return ___lockOwner;
    }

    @Override
    //todo: make final
    public boolean ___tryLock(Transaction lockOwner) {
        //uses a TTAS.

        if (___lockOwner != null) {
            return false;
        }

        return ___LOCKOWNER_UPDATER.compareAndSet(this, null, lockOwner);
    }

    @Override
    //todo: make final
    public void ___releaseLock(Transaction expectedLockOwner) {
        //uses a TTAS.

        if (___lockOwner == expectedLockOwner) {
            ___LOCKOWNER_UPDATER.set(this, null);
        }
    }

    //============================== retry functionality ========================

    public final Listeners ___getListeners() {
        return ___listeners;
    }

    @Override
    public final RegisterRetryListenerResult ___registerRetryListener(Latch listener, long minimumWakeupVersion) {
        AlphaTranlocal tranlocalT1 = ___tranlocal;

        //could it be that a locked value is read? (YES, can happen) A value that will be updated,
        //but isn't updated yet.. consequence: the listener tries to registerLifecycleListener a listener.
        if (tranlocalT1 == null) {
            //no tranlocal has been committed yet. We don't need to registerLifecycleListener the listener,
            //because this call can only be made a transaction that has newattached items
            //and does an abort.

            //todo: one thing we still need to take care of is the noprogresspossible exception
            //if all objects within the transaction give this behavior it looks like the
            //latch was registered, but the transaction will never be woken up.
            return RegisterRetryListenerResult.noregistration;
        } else if (tranlocalT1.getWriteVersion() >= minimumWakeupVersion) {
            //if the version if the tranlocal already is equal or bigger than the version we
            //are looking for, we are done.
            listener.open();
            return RegisterRetryListenerResult.opened;
        } else {
            //ok, the version we are looking for has not been committed yet, so we need to
            //registerLifecycleListener a the listener so that it will be opened

            boolean placedListener;
            Listeners newListeners;
            Listeners oldListeners;
            do {
                oldListeners = ___LISTENERS_UPDATER.get(this);
                newListeners = new Listeners(listener, oldListeners);
                placedListener = ___LISTENERS_UPDATER.compareAndSet(this, oldListeners, newListeners);
                if (!placedListener) {
                    //it could be that another transaction did a registerLifecycleListener, but it also could mean
                    //that a write occurred.
                    AlphaTranlocal tranlocalT2 = ___tranlocal;
                    if (tranlocalT1 != tranlocalT2) {
                        //we are not sure when the registration took place, but a new version is available.
                        //a write happened so we can closed this latch
                        listener.open();
                        return RegisterRetryListenerResult.opened;
                    }
                }
            } while (!placedListener);

            AlphaTranlocal tranlocalT2 = ___tranlocal;
            if (tranlocalT1 != tranlocalT2) {
                listener.open();
                //lets try to restore the oldListeners.
                ___LISTENERS_UPDATER.compareAndSet(this, newListeners, oldListeners);
            }
            //else: it is registered before the write took place

            return RegisterRetryListenerResult.registered;
        }
    }
}
