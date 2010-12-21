package org.multiverse.api;

import org.multiverse.MultiverseConstants;

/**
 * With the LockLevel one can influence various levels of pessimistic behavior in the Stm.
 * For more information on configuration see {@link TransactionFactoryBuilder#setLockLevel(LockLevel)}.
 *
 * @author Peter Veentjer.
 */
public enum LockLevel implements MultiverseConstants {

    /**
     * A LockLevel that commit locks all writes. Once the commit lock is acquired, it isn't readable
     * by any other transaction.
     */
    CommitLockWrites(LOCKMODE_NONE, LOCKMODE_COMMIT),

    /**
     * A LockLevel that commit locks reads (and therefor all writes). It is the most
     * strict LockLevel. Once the commit lock is acquired, it isn't readable or writable by any other transaction.
     */
    CommitLockReads(LOCKMODE_COMMIT, LOCKMODE_COMMIT),

    /**
     * A LockLevel that write locks all writes. This is the default behavior you get when Oracle is used where
     * each insert/update/delete is locked for the remaining duration of the transaction. If something is write
     * locked, it can't be updated by another transaction, but still can be read.
     */
    WriteLockWrites(LOCKMODE_NONE, LOCKMODE_UPDATE),

    /**
     * A LockLevel that write locks all reads (and therefor all writes).
     */
    WriteLockReads(LOCKMODE_UPDATE, LOCKMODE_UPDATE),

    /**
     * A LockLevel that doesn't require any locking. This is the default.
     */
    LockNone(LOCKMODE_NONE, LOCKMODE_NONE);

    private final int readLockMode;
    private final int writeLockMode;

    private LockLevel(int readLockMode, int writeLockMode) {
        this.readLockMode = readLockMode;
        this.writeLockMode = writeLockMode;
    }

    /**
     * Checks if this LockLevel requires the lock of a write.
     *
     * @return true if it requires the lock of a write.
     */
    public final int getWriteLockMode() {
        return writeLockMode;
    }

    /**
     * Checks if this LockLevel requires the lock of a read (and also the lock of a write since
     * a read is needed before doing a write).
     *
     * @return true if it requires the lock of a read.
     */
    public final int getReadLockMode() {
        return readLockMode;
    }
}
