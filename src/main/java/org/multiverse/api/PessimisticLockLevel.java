package org.multiverse.api;

/**
 * With the PessimisticLockLevel one can influence various levels of pessimistic behavior in the Stm.
 * For more information on configuration see
 * {@link TransactionFactoryBuilder#setPessimisticLockLevel(PessimisticLockLevel)}.
 *
 * @author Peter Veentjer.
 */
public enum PessimisticLockLevel {

    /**
     * A PessimisticLockLevel that requires the locks of all writes.
     */
    Write(false, true),

    /**
     * A PessimisticLockLevel that requires the locks of all reads (and therefor all writes). It is the most
     * strict PessimisticLockLevel.
     */
    Read(true, true),

    /**
     * A PessimisticLockLevel that doesn't require any locking.
     */
    None(false, false),

    /**
     * Exclusively locks (so a read is not possible when a write is done).
     */
    Exclusive(true, true);

    private final boolean lockReads;
    private final boolean lockWrites;

    private PessimisticLockLevel(boolean lockReads, boolean lockWrites) {
        this.lockReads = lockReads;
        this.lockWrites = lockWrites;
    }

    /**
     * Checks if this PessimisticLockLevel requires the lock of a write.
     *
     * @return true if it requires the lock of a write.
     */
    public final boolean lockWrites() {
        return lockWrites;
    }

    /**
     * Checks if this PessimisticLockLevel requires the lock of a read (and also the lock of a write since
     * a read is needed before doing a write).
     *
     * @return true if it requires the lock of a read.
     */
    public final boolean lockReads() {
        return lockReads;
    }
}
