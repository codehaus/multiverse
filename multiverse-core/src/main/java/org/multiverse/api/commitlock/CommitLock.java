package org.multiverse.api.commitlock;

import org.multiverse.api.Transaction;

/**
 * The {@link CommitLock} should never by acquired directly, but always through the {@link CommitLockPolicy}. The
 * implementation of the lock are not supposed to use techniques like spinning, that should all be part of the
 * lockpolicy. The lock implementation should try to acquire the lock and fail immediately if it can't.
 * <p/>
 * An element in a LockSet (an array of LockSetElements).
 * <p/>
 * A null array is equal to an array of length 0. items are always placed from the left to the right. as soon as a null
 * element is found, the items on the right don't need to be tried.
 * <p/>
 * It could be that after a null element, there still are junk objects from the past.
 *
 * @author Peter Veentjer.
 */
public interface CommitLock {

    /**
     * Returns the current owner of the lock, or null if not locked.
     *
     * @return the current owner, or null if lock is free.
     */
    Transaction ___getLockOwner();

    /**
     * Tries to acquire the lock. Atm when the lock already is acquired, this call is going to fail.
     * <p/>
     * CommitLocks are not reentrant.
     *
     * @param lockOwner the Transaction that wants to own the lock.
     * @return true if the lock was acquired, false otherwise.
     */
    boolean ___tryLock(Transaction lockOwner);

    /**
     * Releases the lock under the condition that it was owned by the expectedLockOwner. If a different Transaction
     * owns, the lock, the lock should not be released.
     * <p/>
     * It is very important that this method doesn't fail because when it does it could leave locks locked.
     *
     * @param expectedLockOwner the expected Transaction that owns the lock.
     */
    void ___releaseLock(Transaction expectedLockOwner);
}
