package org.multiverse.stms.alpha.mixins;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.Latch;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LoadLockedException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.alpha.AlphaStmUtils;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.RegisterRetryListenerResult;
import org.multiverse.utils.Listeners;

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
public abstract class DefaultTxObjectMixin implements AlphaTransactionalObject, MultiverseConstants {

    private final static AtomicReferenceFieldUpdater<DefaultTxObjectMixin, Transaction> ___LOCKOWNER_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DefaultTxObjectMixin.class, Transaction.class, "___lockOwner");

    private final static AtomicReferenceFieldUpdater<DefaultTxObjectMixin, AlphaTranlocal> ___TRANLOCAL_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DefaultTxObjectMixin.class, AlphaTranlocal.class, "___tranlocal");

    private final static AtomicReferenceFieldUpdater<DefaultTxObjectMixin, Listeners> ___LISTENERS_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DefaultTxObjectMixin.class, Listeners.class, "___listeners");

    private volatile Transaction ___lockOwner;
    private volatile AlphaTranlocal ___tranlocal;
    private volatile Listeners ___listeners;

    @Override
    public AlphaTranlocal ___load() {
        return ___TRANLOCAL_UPDATER.get(this);
    }

    public Listeners ___getListeners() {
        return ___listeners;
    }

    @Override
    public AlphaTranlocal ___load(long readVersion) {
        if (___LOCKOWNER_UPDATER.get(this) != null) {
            throw LoadLockedException.INSTANCE;
        }

        AlphaTranlocal tranlocalTime1 = ___TRANLOCAL_UPDATER.get(this);

        if (tranlocalTime1 == null) {
            //a read is done, but there is no committed data. Lets return null.
            return null;
        }

        if (___SANITY_CHECKS_ENABLED) {
            if (tranlocalTime1.isUncommitted()) {
                throw new PanicError();
            }
        }
        //since the version is directly connected to the tranlocal, we don't need to worry that we
        //see tranlocal with a wrong version. Something you need to watch out for if they are stored
        //in seperate (cas)fields.

        //If the tranlocal is exactly the one we look for, it doesn't matter if it is still locked. It is
        //going to be committed eventually and waiting for it, or retrying the transaction, would have
        //no extra value.

        if (tranlocalTime1.getWriteVersion() == readVersion) {
            //we are lucky, the tranlocal is exactly the one we are looking for.
            return tranlocalTime1;
        } else if (tranlocalTime1.getWriteVersion() > readVersion) {
            //the current tranlocal it too new to return, so we fail. In the future this would
            //be the location to search for tranlocal with the correct version.
            if (LoadTooOldVersionException.reuse) {
                throw LoadTooOldVersionException.INSTANCE;
            } else {
                String msg = format(
                        "Can't load version '%s' for transactionalobject '%s', the oldest version found is '%s'",
                        readVersion,
                        AlphaStmUtils.toTxObjectString(this),
                        tranlocalTime1.getWriteVersion());
                throw new LoadTooOldVersionException(msg);
            }
        } else {
            Transaction lockOwner = ___LOCKOWNER_UPDATER.get(this);

            if (lockOwner != null) {
                //this would be the location for spinning. As long as the lock is there,
                //we are not sure if the version read is the version that can be returned (perhaps there are
                //pending writes).
                if (LoadLockedException.reuse) {
                    throw LoadLockedException.INSTANCE;
                } else {
                    String msg = format("Failed to load already locked transactionalobject '%s'",
                            AlphaStmUtils.toTxObjectString(this));
                    throw new LoadLockedException(msg);
                }
            }

            AlphaTranlocal tranlocalTime2 = ___TRANLOCAL_UPDATER.get(this);
            boolean otherWritesHaveBeenExecuted = tranlocalTime2 != tranlocalTime1;
            if (otherWritesHaveBeenExecuted) {
                //if the tranlocal has changed, lets check if the new tranlocal has exactly the
                //version we are looking for.
                if (tranlocalTime2.getWriteVersion() == readVersion) {
                    return tranlocalTime2;
                }

                //we were not able to find the version we are looking for. It could be tranlocalT1
                //or tranlocalT2 but it could also have been a write we didn't notice. So lets
                //fails to indicate that we didn't find it.
                if (LoadTooOldVersionException.reuse) {
                    throw LoadTooOldVersionException.INSTANCE;
                } else {
                    String msg = format(
                            "Can't load version '%s' transactionalobject '%s', the oldest version found is '%s'",
                            readVersion,
                            AlphaStmUtils.toTxObjectString(this),
                            tranlocalTime2.getWriteVersion());
                    throw new LoadTooOldVersionException(msg);
                }
            } else {
                //the tranlocal has not changed and it was unlocked. This means that we read
                //an old version that we can use.
                return tranlocalTime1;
            }
        }
    }

    @Override
    public Transaction ___getLockOwner() {
        return ___lockOwner;
    }

    @Override
    public boolean ___tryLock(Transaction lockOwner) {
        return ___LOCKOWNER_UPDATER.compareAndSet(this, null, lockOwner);
    }

    @Override
    public void ___releaseLock(Transaction expectedLockOwner) {
        //todo: here is where contention could happen.. 
        //idea for performance improvement based on 'the art of multiprocessor programming
        //chapter 7.2 change to: TTAS (Test-Test-And-Swap) 
        ___LOCKOWNER_UPDATER.compareAndSet(this, expectedLockOwner, null);
    }

    @Override
    public Listeners ___store(AlphaTranlocal tranlocal, long writeVersion) {
        assert tranlocal != null;

        if (___SANITY_CHECKS_ENABLED) {
            if (___lockOwner == null) {
                String msg = format("Lock on transactionalObject '%s' is not hold while doing the store",
                        AlphaStmUtils.toTxObjectString(this));
                throw new PanicError(msg);
            }

            if (tranlocal.getWriteVersion() >= writeVersion) {
                String msg = format("The tranlocal of transactionalObject '%s' has version '%s'  " +
                        "and and is too large for writeVersion '%s'",
                        AlphaStmUtils.toTxObjectString(this),
                        tranlocal.getTransactionalObject(),
                        writeVersion);
                throw new PanicError(msg);
            }

            AlphaTranlocal old = ___TRANLOCAL_UPDATER.get(this);
            if (old != null && old.getWriteVersion() >= writeVersion) {
                String msg = format(
                        "The current version '%s' is newer than the version '%s' to commit for transactionalobject '%s''",
                        old.getWriteVersion(),
                        writeVersion,
                        tranlocal.getWriteVersion());
                throw new PanicError(msg);
            }
        }

        //it is very important that the tranlocal write is is done before the lock release.
        //it also is very important that the commit and version are set, before the tranlocal write.
        //the tranlocal write also creates a happens before relation between the changes made on the
        //tranlocal, and the read on the tranlocal.
        tranlocal.prepareForCommit(writeVersion);

        ___TRANLOCAL_UPDATER.set(this, tranlocal);

        //it is important that the listeners are removed after the tranlocal write en before the lockrelease.
        Listeners listeners = ___LISTENERS_UPDATER.getAndSet(this, null);

        //release the listeners
        // ___LOCKOWNER_UPDATER.set(this, null);

        return listeners;

        //return null;
    }


    @Override
    public RegisterRetryListenerResult ___registerRetryListener(Latch listener, long minimumWakeupVersion) {
        AlphaTranlocal tranlocalT1 = ___TRANLOCAL_UPDATER.get(this);

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
                    AlphaTranlocal tranlocalT2 = ___TRANLOCAL_UPDATER.get(this);
                    if (tranlocalT1 != tranlocalT2) {
                        //we are not sure when the registration took place, but a new version is available.

                        if (___SANITY_CHECKS_ENABLED) {
                            if (tranlocalT2.getWriteVersion() <= tranlocalT1.getWriteVersion()) {
                                String msg = format(
                                        "Going back in time; transactionalobject '%s' and tranlocalT2 with version" +
                                                " '%s' has a smaller version than tranlocalT2 with version '%s'",
                                        AlphaStmUtils.toTxObjectString(this),
                                        tranlocalT1.getWriteVersion(),
                                        tranlocalT2.getWriteVersion());
                                throw new PanicError(msg);
                            }

                            if (minimumWakeupVersion > tranlocalT2.getWriteVersion()) {
                                String msg = format(
                                        "Minimum version '%s' for registerRetryListener on transactionalobject '%s' is larger"
                                                +
                                                " than tranlocalT2.version '%s'",
                                        minimumWakeupVersion,
                                        AlphaStmUtils.toTxObjectString(this),
                                        tranlocalT2.getWriteVersion());
                                throw new PanicError(msg);
                            }
                        }
                        //a write happened so we can closed this latch
                        listener.open();
                        return RegisterRetryListenerResult.opened;
                    }
                }
            } while (!placedListener);

            AlphaTranlocal tranlocalT2 = ___TRANLOCAL_UPDATER.get(this);
            if (tranlocalT1 != tranlocalT2) {
                if (___SANITY_CHECKS_ENABLED) {
                    //we are not sure when the registration took place, but a new version is available.
                    if (tranlocalT2.getWriteVersion() < minimumWakeupVersion) {
                        String msg = format(
                                "TranlocalT2 with version '%s' for registerRetryListener on transactionalobject '%s' is smaller"
                                        +
                                        " than minimumWakeupVersion '%s'",
                                tranlocalT2.getWriteVersion(),
                                AlphaStmUtils.toTxObjectString(this),
                                minimumWakeupVersion);
                        throw new PanicError(msg);
                    }
                }
                listener.open();
                //lets try to restore the oldListeners.
                ___LISTENERS_UPDATER.compareAndSet(this, newListeners, oldListeners);
            }
            //else: it is registered before the write took place

            return RegisterRetryListenerResult.registered;
        }
    }
}
