package org.multiverse.stms.alpha;

import org.multiverse.api.Transaction;
import org.multiverse.utils.Listeners;
import org.multiverse.utils.latches.Latch;

/**
 * This is the interface placed on the POJO's that want to participate in the alpha STM.
 * <p/>
 * In most cases a user of the library is not going to worry bout this interface. Instrumentation
 * is going to do all that work based on annotations.
 *
 * @author Peter Veentjer.
 */
public interface AlphaAtomicObject {

    /**
     * Loads the {@link AlphaTranlocal} with a version equal or smaller than readVersion. It is very
     * important for the implementation to not to return a too old version. If this happens, the
     * system could start to suffer from lost updates (not seeing changes you should have seen).
     *
     * The returned instance can't be used for updates. See the {@link #___loadUpdatable(long)}.
     *
     * @param readVersion the version of the Tranlocal to read.
     * @return the loaded Tranlocal. If nothing is committed, null is returned.
     * @throws org.multiverse.api.exceptions.LoadException
     *          if the system wasn't able to load the Tranlocal.
     */
    AlphaTranlocal ___load(long readVersion);

    /**
     * Loads the most recently committed AlphaTranlocal.
     *
     * @return the most recently written AlphaTranlocal.
     * @throws org.multiverse.api.exceptions.LoadException
     *          if the system wasn't able to load the Tranlocal.
     */
    AlphaTranlocal ___load();

    AlphaTranlocal ___loadUpdatable(long readVersion);

    /**
     * Acquires the lock. The lock is only acquired it the lock is free.
     *
     * @param lockOwner the owner of the lock.
     * @return true if the lock was acquired, false otherwise.
     */
    boolean ___tryLock(Transaction lockOwner);

    /**
     * Releases the lock. The lock is only released if the current lockOwner is equal to the expected
     * lock owner. If the lock is free, this call is ignored. If the lock is owned by a different transaction
     * this call is ignored.
     * <p/>
     * It is important that this method always completes. If it doesn't, it could leave memory
     * in an inconsistent state (locked) and therefor useless.
     *
     * @param expectedLockOwner the expected LockOwner.
     */
    void ___releaseLock(Transaction expectedLockOwner);

    /**
     * Stores the the content and releases the lock. It also removes listeners that should be triggered
     * by this store. The caller is responsible for taking care of waking up the listeners.
     * <p/>
     * It is important that this call only is made when the lock already was acquired.
     *
     * @param tranlocal    the Tranlocal to storeAndReleaseLock.
     * @param writeVersion the version to storeAndReleaseLock the Tranlocal with.
     * @return the Listeners to wake up. Could be null if there are no listeners to wake up.
     */
    Listeners ___storeAndReleaseLock(AlphaTranlocal tranlocal, long writeVersion);

    /**
     * Registers a listener for retrying (the condition variable version for STM's). The Latch is a
     * concurrency structure that can be used to let a thread (transaction) wait for a specific event.
     * In this case we use it to notify the Transaction that the desired update has taken place.
     *
     * @param listener             the Latch to register.
     * @param minimumWakeupVersion the minimum version of the data
     * @return true if the listener was registered on a committed object, false otherwise.
     */
    boolean ___registerRetryListener(Latch listener, long minimumWakeupVersion);

    /**
     * Returns the current owner of the lock, or null if AtomicObject is not locked.
     *
     * @return the current owner, or null if lock is free.
     */
    Transaction ___getLockOwner();
}
