package org.multiverse.api;

import org.multiverse.MultiverseConstants;

/**
 * Using the LockMode one can control how pessimistic a transaction is.
 *
 * @author Peter Veentjer.
 */
public enum LockMode implements MultiverseConstants {

    /**
     * No locking is done.
     */
    None(LOCKMODE_NONE),

    /**
     * The LockMode.Read prevents others to acquire the Write/Exclusive-lock, but it allows others to acquire the
     * Read lock. Unlike a traditional read/write-lock, it doesn't have to mean that other transactions can't write,
     * it only prevents others from committing.
     */
    Read(LOCKMODE_READ),

    /**
     * The LockMode.Write prevents others to acquire the Read/Write/Exclusive-lock. Unlike a traditional read-write lock,
     * it doesn't have to mean that other transactions can't read or write, they only can't commit since the Exclusive lock
     * is acquired for that (managed by the STM).
     */
    Write(LOCKMODE_WRITE),

    /**
     * The ExclusiveLock can be compared with the writelock of a traditional read/write lock. once the Exclusive lock is acquired
     * no other transaction can acquire any lock or can do any reading/writing (unless the transaction previously has read the
     * transactional object).
     * <p/>
     * The ExclusiveLock is the Lock acquired by the STM once a Transaction writes changes to a TransactionalObject.
     */
    Exclusive(LOCKMODE_EXCLUSIVE);

    private int lockMode;

    private LockMode(int lockMode) {
        this.lockMode = lockMode;
    }

    public int asInt() {
        return lockMode;
    }
}
