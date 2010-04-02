package org.multiverse.stms.alpha.mixins;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.Latch;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockNotFreeReadConflict;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.RegisterRetryListenerResult;
import org.multiverse.utils.Listeners;
import org.multiverse.utils.TodoException;

/**
 * A threadunsafe tx object mixin that can be used for benchmarking purposes if you want to
 * see what the overhead is caused by cas/volatile read/volatile write operations.
 * <p/>
 * Can't be used in a multithreaded program.
 */
public abstract class ThreadUnsafeTxObjectMixin implements AlphaTransactionalObject, MultiverseConstants {

    private Transaction ___lockOwner;

    private AlphaTranlocal ___tranlocal;

    @Override
    public AlphaTranlocal ___load() {
        return ___tranlocal;
    }

    public Listeners ___getListeners() {
        return null;
    }

    @Override
    public AlphaTranlocal ___load(long readVersion) {
        if (___lockOwner != null) {
            throw LockNotFreeReadConflict.INSTANCE;
        }

        AlphaTranlocal tranlocalTime1 = ___tranlocal;

        if (tranlocalTime1 == null) {
            //a read is done, but there is no committed data. Lets return null.
            return null;
        }

        if (tranlocalTime1.getWriteVersion() == readVersion) {
            //we are lucky, the tranlocal is exactly the one we are looking for.
            return tranlocalTime1;
        } else if (tranlocalTime1.getWriteVersion() > readVersion) {
            //the current tranlocal it too new to return, so we fail. In the future this would
            //be the location to search for tranlocal with the correct version.
            throw OldVersionNotFoundReadConflict.INSTANCE;
        } else {
            Transaction lockOwner = ___lockOwner;

            if (lockOwner != null) {
                //this would be the location for spinning. As long as the lock is there,
                //we are not sure if the version read is the version that can be returned (perhaps there are
                //pending writes).
                throw LockNotFreeReadConflict.INSTANCE;
            }

            AlphaTranlocal tranlocalTime2 = ___tranlocal;
            boolean otherWritesHaveBeenExecuted = tranlocalTime2 != tranlocalTime1;
            if (otherWritesHaveBeenExecuted) {
                //if the tranlocal has changed, lets check if the new tranlocal has exactly the
                //version we are looking for.
                if (tranlocalTime2.getWriteVersion() == readVersion) {
                    return tranlocalTime2;
                }

                throw OldVersionNotFoundReadConflict.INSTANCE;
            } else {
                //the tranlocal has not changed and it was unlocked. This means that we read
                //an old version that we can use.
                return tranlocalTime1;
            }
        }
    }

    @Override
    public AlphaTranlocal ___openForCommutingOperation() {
        throw new TodoException();
    }

    @Override
    public Transaction ___getLockOwner() {
        return ___lockOwner;
    }

    @Override
    public boolean ___tryLock(Transaction lockOwner) {
        if (___lockOwner != null) {
            return false;
        }

        ___lockOwner = lockOwner;
        return true;
    }

    @Override
    public void ___releaseLock(Transaction expectedLockOwner) {
        if (___lockOwner == expectedLockOwner) {
            ___lockOwner = null;
        }
    }

    @Override
    public AlphaTranlocal ___openUnconstructed() {
        return null;  //todo
    }

    @Override
    public Listeners ___storeUpdate(AlphaTranlocal tranlocal, long writeVersion, boolean releaseLock) {
        assert tranlocal != null;
        tranlocal.prepareForCommit(writeVersion);
        ___tranlocal = tranlocal;
        if (releaseLock) {
            ___lockOwner = null;
        }
        return null;
    }

    @Override
    public void ___storeInitial(AlphaTranlocal tranlocal, long writeVersion) {
        tranlocal.prepareForCommit(writeVersion);
        ___tranlocal = tranlocal;
    }

    @Override
    public RegisterRetryListenerResult ___registerRetryListener(Latch listener, long minimumWakeupVersion) {
        return RegisterRetryListenerResult.noregistration;
    }
}
