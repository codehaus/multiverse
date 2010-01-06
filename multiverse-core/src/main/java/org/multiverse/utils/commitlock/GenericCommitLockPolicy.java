package org.multiverse.utils.commitlock;

import org.multiverse.api.Transaction;
import static org.multiverse.utils.commitlock.CommitLockUtils.nothingToLock;

import static java.lang.String.format;

/**
 * An {@link CommitLockPolicy} that spins when it can't acquire a lock. When the lock can't
 * be acquired, all locks are released and the locks are tries to be acquired again. The number
 * of spins and retries can be configured. So you can create a version that doesn't spin, but doesn't
 * retry, or a version that does spin but doesn't retry, etc.
 * <p/>
 * Because spinning increases the time a lock is hold, it could prevent other transactions from
 * making progress. So be careful. Setting the retry level too high, could lead to livelocking, but
 * on the other side it could also cause an increase in failure rates of transactions and also
 * cause livelocking on transaction level. So finding good value's is something that needs to be
 * determined.
 * <p/>
 * This GenericAtomicObjectLockPolicy is immutable and thread-safe to use.
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
    public CommitLockResult tryLockAndDetectConflict(CommitLock lock, Transaction lockOwner) {
        if (lockOwner == null) {
            throw new NullPointerException();
        } else if (lock == null) {
            return CommitLockResult.success;
        } else {
            return lock.tryLockAndDetectConflicts(lockOwner);
        }
    }

    public CommitLockResult tryLockAllAndDetectConflicts(CommitLock[] locks, Transaction lockOwner) {
        if (lockOwner == null) {
            throw new NullPointerException();
        } else if (nothingToLock(locks)) {
            return CommitLockResult.success;
        } else {
            int maxAttempts = 1 + retryCount;
            int attempt = 1;

            while (attempt <= maxAttempts) {
                switch (attempt(locks, lockOwner)) {
                    case success:
                        return CommitLockResult.success;
                    case failure:
                        attempt++;
                        break;
                    case conflict:
                        return CommitLockResult.conflict;
                    default:
                        throw new IllegalStateException();
                }
            }

            return CommitLockResult.failure;
        }
    }

    /**
     * A single attempt to acquire all the locks.
     *
     * @param locks     the AtomicObjectLocks to acquire.
     * @param lockOwner the Transaction that wants to own the locks.
     * @return true if it was a success, false otherwise.
     */
    private CommitLockResult attempt(CommitLock[] locks, Transaction lockOwner) {
        int money = 0;

        boolean locksNeedToBeReleased = true;
        int lockIndex = 0;
        try {
            for (lockIndex = 0; lockIndex < locks.length; lockIndex++) {
                CommitLock lock = locks[lockIndex];
                if (lock == null) {
                    locksNeedToBeReleased = false;
                    return CommitLockResult.success;
                } else {
                    boolean lockAcquired;
                    do {
                        switch (lock.tryLockAndDetectConflicts(lockOwner)) {
                            case success:
                                lockAcquired = true;
                                break;
                            case failure:
                                lockAcquired = false;
                                money--;
                                if (money < 0) {
                                    lockIndex--;
                                    return CommitLockResult.failure;
                                }
                                break;
                            case conflict:
                                return CommitLockResult.conflict;
                            default:
                                throw new IllegalStateException();
                        }

                    } while (!lockAcquired);

                    money += spinAttemptsPerLockCount;
                }
            }

            locksNeedToBeReleased = false;
            return CommitLockResult.success;
        } finally {
            if (locksNeedToBeReleased) {
                releaseLocks(locks, lockOwner, lockIndex);
            }
        }
    }

    private void releaseLocks(CommitLock[] locks, Transaction owner, int lastIndexOfLockToRelease) {
        for (int k = 0; k <= lastIndexOfLockToRelease; k++) {
            CommitLock lock = locks[k];
            lock.releaseLock(owner);
        }
    }

    @Override
    public String toString() {
        return format("GenericCommitLockPolicy(retryCount=%s, spinAttemptsPerLockCount=%s)",
                retryCount, spinAttemptsPerLockCount);
    }
}

