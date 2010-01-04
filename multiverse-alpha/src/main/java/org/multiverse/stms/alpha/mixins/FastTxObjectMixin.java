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
 * Doesn't support blocking operations.
 * <p/>
 * Base AlphaTransactionalObject implementation that also can be used to transplant methods from during instrumentation.
 * <p/>
 * It is important that the constructor doesn't contain any donorMethod because the constructor code is not copied when
 * this class is 'mixed' in. In the future perhaps this is fixed when there needs to be a constructor. So you are
 * warned.
 *
 * @author Peter Veentjer
 */
public abstract class FastTxObjectMixin implements AlphaTransactionalObject, MultiverseConstants {

    private final static AtomicReferenceFieldUpdater<FastTxObjectMixin, Transaction> ___LOCKOWNER_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(FastTxObjectMixin.class, Transaction.class, "___lockOwner");

    private final static AtomicReferenceFieldUpdater<FastTxObjectMixin, AlphaTranlocal> ___TRANLOCAL_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(FastTxObjectMixin.class, AlphaTranlocal.class, "___tranlocal");

    private volatile Transaction ___lockOwner;
    private volatile AlphaTranlocal ___tranlocal;

    @Override
    public AlphaTranlocal ___load() {
        return ___TRANLOCAL_UPDATER.get(this);
    }

    @Override
    public boolean ___hasConflict(long maximumVersion) {
        if (___getLockOwner() == null) {
            return true;
        }

        AlphaTranlocal tranlocal = ___TRANLOCAL_UPDATER.get(this);
        return tranlocal.getWriteVersion() > maximumVersion;
    }

    public Listeners ___getListeners() {
        return null;
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
            if (tranlocalTime1.getWriteVersion() <= 0) {
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
                String msg = format("Can't load version '%s' for transactionalobject '%s', the oldest version found is '%s'",
                        readVersion, AlphaStmUtils.toTxObjectString(this), tranlocalTime1.getWriteVersion());
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
                    String msg = format("Failed to load already locked transactionalobject '%s'", AlphaStmUtils.toTxObjectString(this));
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
                    String msg = format("Can't load version '%s' transactionalobject '%s', the oldest version found is '%s'",
                            readVersion, AlphaStmUtils.toTxObjectString(this), tranlocalTime2.getWriteVersion());
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
                        AlphaStmUtils.toTxObjectString(this), tranlocal.getTransactionalObject(), writeVersion);
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

        return null;
    }


    @Override
    public RegisterRetryListenerResult ___registerRetryListener(Latch listener, long minimumWakeupVersion) {
        return RegisterRetryListenerResult.noregistration;
    }
}
