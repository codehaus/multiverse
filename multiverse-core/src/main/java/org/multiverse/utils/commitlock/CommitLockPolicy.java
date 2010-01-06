package org.multiverse.utils.commitlock;

import org.multiverse.api.Transaction;

/**
 * A policy responsible for acquiring the lock of a set of AtomicObjectLocks.
 * <p/>
 * When a Transaction commits, it needs to acquire locks on the objects of the writeset.
 * With this LockPolicy this behavior can be influenced.
 * <p/>
 * If the locks could not be acquired, the policy is responsible for making sure that
 * all locks are released. So a callee is guaranteed to own all locks or no locks at all.
 * <p/>
 * The reason that the WriteSetLockPolicy works with an array, is that it doesn't create
 * a lot of gc-litter.
 *
 * @author Peter Veentjer.
 */
public interface CommitLockPolicy {

    CommitLockResult tryLockAndDetectConflict(CommitLock lock, Transaction lockOwner);

    /**
     * Tries to acquire all the locks.
     * <p/>
     * Acquires the locks for a writeset. The semantics of the array:
     * - if the writeset is null or length 0, the result = true.
     * - as soon as the writeset contains a null, there are no other items and
     * - true can be returned.
     * - for all non null elements the lock needs to be tried to acquire.
     *
     * @param locks     the AtomicObjectLock to acquire.
     * @param lockOwner the Transaction that wants to own the lock.
     * @return true if the locks are acquired successfully, false otherwise.
     */
    CommitLockResult tryLockAllAndDetectConflicts(CommitLock[] locks, Transaction lockOwner);
}
