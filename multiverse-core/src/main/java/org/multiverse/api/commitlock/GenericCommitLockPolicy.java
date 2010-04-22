package org.multiverse.api.commitlock;

import org.multiverse.api.Transaction;

import java.util.Collection;

import static java.lang.String.format;

/**
 * An {@link CommitLockPolicy} that spins when it can't acquire a lock. When the lock can't
 * be acquired, all locks are released and the locks are tries to be acquired again. The number
 * of spins and retries can be configured. So you can createReference a version that doesn't spin, but doesn't
 * retry, or a version that does spin but doesn't retry, etc.
 * <p/>
 * Because spinning increases the time a lock is hold, it could prevent other transactions from
 * making progress. So be careful. Setting the retry level too high, could lead to livelocking, but
 * on the other side it could also cause an increase in failure rates of transactions and also
 * cause livelocking on transaction level. So finding good value's is something that needs to be
 * determined.
 * <p/>
 * This GenericCommitLockPolicy is immutable and thread-safe to use.
 *
 * @author Peter Veentjer
 */
public final class GenericCommitLockPolicy implements CommitLockPolicy {

    public static final CommitLockPolicy FAIL_FAST = new GenericCommitLockPolicy(0, 0);
    public static final CommitLockPolicy FAIL_FAST_BUT_RETRY = new GenericCommitLockPolicy(0, 10);
    public static final CommitLockPolicy SPIN_AND_RETRY = new GenericCommitLockPolicy(10, 10);

    private final int spinAttemptsPerLockCount;
    private final int retryCount;

    public GenericCommitLockPolicy(int spinAttemptsPerLockCount, int retryCount) {
        if (spinAttemptsPerLockCount < 0) {
            throw new IllegalArgumentException();
        }

        if (retryCount < 0) {
            throw new IllegalArgumentException();
        }

        this.spinAttemptsPerLockCount = spinAttemptsPerLockCount;
        this.retryCount = retryCount;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getSpinAttemptsPerLockCount() {
        return spinAttemptsPerLockCount;
    }

    @Override
    public boolean tryAcquire(CommitLock lock, CommitLockFilter filter, Transaction lockOwner) {
        if (lockOwner == null) {
            throw new NullPointerException();
        }

        for (int k = 0; k < retryCount; k++) {
            if (singleLock(lock, filter, lockOwner, spinAttemptsPerLockCount) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static int singleLock(CommitLock lock, CommitLockFilter filter, Transaction lockOwner, int availableAttempts) {
        if (lock == null) {
            return availableAttempts;
        }

        if (!filter.needsLocking(lock)) {
            return availableAttempts;
        }

        for (int k = 0; k <= availableAttempts; k++) {
            if (lock.___tryLock(lockOwner)) {
                return availableAttempts - k;
            }
        }

        return -1;
    }

    public boolean tryAcquireAll(CommitLock[] locks, CommitLockFilter filter, Transaction lockOwner) {
        if (lockOwner == null) {
            throw new NullPointerException();
        }

        int maxAttempts = 1 + retryCount;
        int attempt = 1;

        while (attempt <= maxAttempts) {
            if (attempt(locks, filter, lockOwner)) {
                return true;
            }
            attempt++;
        }

        return false;
    }

    public boolean tryAcquireAll(Collection<CommitLock> locks, CommitLockFilter filter, Transaction lockOwner) {
        if (lockOwner == null) {
            throw new NullPointerException();
        }

        int maxAttempts = 1 + retryCount;
        int attempt = 1;

        while (attempt <= maxAttempts) {
            if (attempt(locks, filter, lockOwner)) {
                return true;
            }
            attempt++;
        }

        return false;
    }


    /**
     * A single attempt to acquire all the locks.
     *
     * @param locks     the CommitLocks to acquire.
     * @param lockOwner the Transaction that wants to own the locks.
     * @return true if it was a success, false otherwise.
     */
    private boolean attempt(CommitLock[] locks, CommitLockFilter commitLockFilter, Transaction lockOwner) {
        int money = 0;

        boolean locksNeedToBeReleased = true;
        int lockIndex = 0;
        try {
            for (lockIndex = 0; lockIndex < locks.length; lockIndex++) {
                CommitLock lock = locks[lockIndex];

                if (lock == null) {
                    locksNeedToBeReleased = false;
                    return true;
                }

                if (commitLockFilter.needsLocking(lock)) {
                    boolean lockAcquired;
                    do {
                        if (lock.___tryLock(lockOwner)) {
                            lockAcquired = true;
                        } else {

                            lockAcquired = false;
                            money--;
                            if (money < 0) {
                                lockIndex--;
                                return false;
                            }
                        }
                    } while (!lockAcquired);

                    money += spinAttemptsPerLockCount;
                }
            }

            locksNeedToBeReleased = false;
            return true;
        } finally {
            if (locksNeedToBeReleased) {
                releaseLocks(locks, lockOwner, lockIndex);
            }
        }
    }


    private boolean attempt(Collection<CommitLock> locks, CommitLockFilter commitLockFilter, Transaction lockOwner) {
        int money = 0;

        boolean locksNeedToBeReleased = true;
        try {
            for (CommitLock lock : locks) {
                if (commitLockFilter.needsLocking(lock)) {
                    boolean lockAcquired;
                    do {
                        if (lock.___tryLock(lockOwner)) {
                            lockAcquired = true;
                        } else {
                            lockAcquired = false;
                            money--;
                            if (money < 0) {
                                return false;
                            }
                        }
                    } while (!lockAcquired);

                    money += spinAttemptsPerLockCount;
                }
            }

            locksNeedToBeReleased = false;
            return true;
        } finally {
            if (locksNeedToBeReleased) {
                releaseLocks(locks, lockOwner);
            }
        }
    }

    private static void releaseLocks(CommitLock[] locks, Transaction owner, int lastIndexOfLockToRelease) {
        for (int k = 0; k <= lastIndexOfLockToRelease; k++) {
            CommitLock lock = locks[k];
            lock.___releaseLock(owner);
        }
    }

    private static void releaseLocks(Collection<CommitLock> locks, Transaction owner) {
        for (CommitLock lock : locks) {
            lock.___releaseLock(owner);
        }
    }

    @Override
    public String toString() {
        return format("GenericCommitLockPolicy(retryCount=%s, spinAttemptsPerLockCount=%s)",
                retryCount, spinAttemptsPerLockCount);
    }
}

