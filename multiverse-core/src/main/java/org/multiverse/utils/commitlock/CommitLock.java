package org.multiverse.utils.commitlock;

import org.multiverse.api.Transaction;

/**
 * The AtomicObjectLock should never by acquired directly, but always through the
 * {@link CommitLockPolicy}. The implementation of the lock
 * are not supposed to use techniques like spinning, that should all be part of the lockpolicy.
 * The lock implementation should try to acquire the lock and fail immediately if it can't.
 * <p/>
 * An element in a LockSet (an array of LockSetElements).
 * <p/>
 * A null array is equal to an array of length 0.
 * items are always placed from the left to the right.
 * as soon as a null element is found, the items on the right don't need to be tried.
 * <p/>
 * It could be that after a null element, there still are junk objects from the past.
 *
 * @author Peter Veentjer.
 */
public interface CommitLock {

    /**
     * Tries to acquire the lock.
     *
     * @param lockOwner the Transaction that wants to own the lock.
     * @return the result of this action (will always be a non null value).
     */
    CommitLockResult tryLockAndDetectConflicts(Transaction lockOwner);

    /**
     * Releases the lock under the condition that it was owned by the expectedLockOwner. If a
     * different Transaction owns, the lock, the lock should not be released.
     * <p/>
     * It is very important that this method doesn't fail because when it does it could leave
     * locks
     *
     * @param expectedLockOwner the expected Transaction that owns the lock.
     */
    void releaseLock(Transaction expectedLockOwner);
}
