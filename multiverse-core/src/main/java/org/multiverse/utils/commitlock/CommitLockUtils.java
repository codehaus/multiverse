package org.multiverse.utils.commitlock;

import org.multiverse.api.Transaction;

/**
 * Contains various functions for commit locks.
 *
 * @author Peter Veentjer.
 */
public final class CommitLockUtils {

    /**
     * Checks if there is nothing to lock. There is nothing to lock when the writeset
     * is null, or when the writeset has length 0 or when the first element is null
     * (the following elements will be null as well).
     *
     * @param writeSet the writeset to check if there is anything to lock
     * @return true if there is nothing to lock, false otherwise.
     */
    public static boolean nothingToLock(CommitLock[] writeSet) {
        return writeSet == null || writeSet.length == 0 || writeSet[0] == null;
    }

    /**
     * Releases the locks. It is important that all locks are released. If this is not done,
     * objects could remain locked and en get inaccessible.
     * <p/>
     * If locks is null, the method completes.
     * the locks needs to be tried from the begin to the end.
     * if a null element is found, all following elements are ignored.
     *
     * @param locks     contains the items to release the locks of.
     * @param lockOwner the Transaction that wants to own the locks.
     * @throws NullPointerException if lockOwner is null.
     */
    public static void releaseLocks(CommitLock[] locks, Transaction lockOwner) {
        if (lockOwner == null) {
            throw new NullPointerException();
        }

        if (locks == null) {
            return;
        }

        for (int k = 0; k < locks.length; k++) {
            CommitLock lock = locks[k];
            if (lock == null) {
                //if the lock is null, we are done 
                return;
            }

            lock.___releaseLock(lockOwner);
        }
    }

    //we don't want instances
    private CommitLockUtils() {
    }
}
