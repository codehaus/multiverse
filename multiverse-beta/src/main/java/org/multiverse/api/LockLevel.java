package org.multiverse.api;

import org.multiverse.MultiverseConstants;

/**
 * With the LockLevel one can influence various levels of pessimistic behavior in the Stm.
 * For more information on configuration see
 * {@link TransactionFactoryBuilder#setLockLevel(LockLevel)}.
 *
 * @author Peter Veentjer.
 */
public enum LockLevel implements MultiverseConstants {

    /**
     * A LockLevel that privatizes all writes.
     */
    PrivatizeWrites(LOCKMODE_NONE, LOCKMODE_COMMIT),

    /**
     * A LockLevel that privatizes reads (and therefor all writes). It is the most
     * strict LockLevel.
     */
    PrivatizeReads(LOCKMODE_COMMIT, LOCKMODE_COMMIT),

    /**
     * A LockLevel that ensures all writes.
     */
    EnsureWrites(LOCKMODE_NONE, LOCKMODE_UPDATE),

    /**
     * A LockLevel that ensures all reads (and therefor all writes).
     */
    EnsureReads(LOCKMODE_UPDATE, LOCKMODE_UPDATE),

    /**
     * A LockLevel that doesn't require any locking. This is the default.
     */
    LockNone(LOCKMODE_NONE, LOCKMODE_NONE);

    private final int lockReads;
    private final int lockWrites;

    private LockLevel(int ensureReads, int ensureWrites) {
        this.lockReads = ensureReads;
        this.lockWrites = ensureWrites;
    }

    /**
     * Checks if this LockLevel requires the lock of a write.
     *
     * @return true if it requires the lock of a write.
     */
    public final int getWriteLockMode() {
        return lockWrites;
    }

    /**
     * Checks if this LockLevel requires the lock of a read (and also the lock of a write since
     * a read is needed before doing a write).
     *
     * @return true if it requires the lock of a read.
     */
    public final int getReadLockMode() {
        return lockReads;
    }
}
