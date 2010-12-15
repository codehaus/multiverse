package org.multiverse.api;

import org.multiverse.MultiverseConstants;

/**
 * With the PessimisticLockLevel one can influence various levels of pessimistic behavior in the Stm.
 * For more information on configuration see
 * {@link TransactionFactoryBuilder#setPessimisticLockLevel(PessimisticLockLevel)}.
 *
 * @author Peter Veentjer.
 */
public enum PessimisticLockLevel implements MultiverseConstants {

    /**
     * A PessimisticLockLevel that privatizes all writes.
     */
    PrivatizeWrites(LOCKMODE_NONE, LOCKMODE_COMMIT),

    /**
     * A PessimisticLockLevel that privatizes reads (and therefor all writes). It is the most
     * strict PessimisticLockLevel.
     */
    PrivatizeReads(LOCKMODE_COMMIT, LOCKMODE_COMMIT),

    /**
     * A PessimisticLockLevel that ensures all writes.
     */
    EnsureWrites(LOCKMODE_NONE, LOCKMODE_UPDATE),

    /**
     * A PessimisticLockLevel that ensures all reads (and therefor all writes).
     */
    EnsureReads(LOCKMODE_UPDATE, LOCKMODE_UPDATE),

    /**
     * A PessimisticLockLevel that doesn't require any locking. This is the default.
     */
    LockNone(LOCKMODE_NONE, LOCKMODE_NONE);

    private final int lockReads;
    private final int lockWrites;

    private PessimisticLockLevel(int ensureReads, int ensureWrites) {
        this.lockReads = ensureReads;
        this.lockWrites = ensureWrites;
    }

    /**
     * Checks if this PessimisticLockLevel requires the lock of a write.
     *
     * @return true if it requires the lock of a write.
     */
    public final int getWriteLockMode() {
        return lockWrites;
    }

    /**
     * Checks if this PessimisticLockLevel requires the lock of a read (and also the lock of a write since
     * a read is needed before doing a write).
     *
     * @return true if it requires the lock of a read.
     */
    public final int getReadLockMode() {
        return lockReads;
    }
}
