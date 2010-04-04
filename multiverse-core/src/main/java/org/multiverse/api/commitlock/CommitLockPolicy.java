package org.multiverse.api.commitlock;

import org.multiverse.api.Transaction;

import java.util.Collection;

/**
 * A policy responsible for acquiring the lock of a set of {@link CommitLock}s.
 * <p/>
 * When a Transaction commits, it needs to acquire locks on the objects of the writeset. With this LockPolicy this
 * behavior can be influenced.
 * <p/>
 * If the locks could not be acquired, the policy is responsible for making sure that all locks are released. So a
 * callee is guaranteed to own all locks or no locks at all.
 * <p/>
 * The reason that the WriteSetLockPolicy works with an array, is that it doesn't createReference a lot of gc-litter.
 * <p/>
 * It is important to realize that locks should be acquired and if that fails, should all be released. The implementer
 * has to realize that ... 2 phase locking (todo: better explanation).
 *
 * @author Peter Veentjer.
 */
public interface CommitLockPolicy {

    /**
     * Tries to acquire the lock.
     *
     * @param lock      the CommitLock to acquire.
     * @param filter    the filter that selects to objects to lock.
     * @param lockOwner the Transaction that wants to own the lock.
     * @return true if the lock is acquired, false otherwise.
     */
    boolean tryAcquire(CommitLock lock, CommitLockFilter filter, Transaction lockOwner);

    /**
     * Tries to acquire all the locks that should be locked (so are allowed by the lock filter).
     * <p/>
     * The filter is useful for dealing with a read/write set where only the writes need to be locked.
     *
     * @param locks     the CommitLocks where some need to be acquired.
     * @param filter    selects which CommitLocks need to be acquired
     * @param lockOwner the Transaction that wants to acquire the locks.
     * @return true if desired locks are acquired, false otherwise.
     */
    boolean tryAcquireAll(CommitLock[] locks, CommitLockFilter filter, Transaction lockOwner);

    boolean tryAcquireAll(Collection<CommitLock> locks, CommitLockFilter filter, Transaction lockOwner);

}
