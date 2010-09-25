package org.multiverse.api;

/**
 * @author Peter Veentjer.
 */
public enum IsolationLevel {

    /**
     * The lowest isolation level where one can see uncommitted data and inconsistent data. Use it carefully
     * since the data read could be something that never entered the system or is inconsistent.
     */
    ReadUncommitted(true, true, true),

    /**
     * This isolation level doesn't allow for uncommitted data to be read, but you don't get any consistency
     * guarantees. It could be that the value read changes over time (although when readtracking is used this
     * problem won't happen that often). And no guarantees are made that the data you read is consistent.
     *
     * Using the ReadCommitted isolation level can be dangerous since the consistency of the data is not guaranteed.
     * This is even more true for and updating transaction since it could leave objects in an inconsistent state. So
     * it it very carefully.
     */
    ReadCommitted(false, true, true),

    /**
     * The default isolation level that allows for the writeskew problem but not for dirty reads or unrepeatable reads.
     * This is the 'serialized' isolation level provided by MVCC databases like Oracle/Postgresql
     * (although Postgresql 9 is going to provide a truly serialized isolation level) and MySQL with the InnoDb.
     * All data read contains committed data and all data will be consistent.
     */
    Snapshot(false, false, true),

    /**
     * Provides truly serialized transaction at the cost of reduced performance and concurrency. This is the highest
     * isolation level where no isolation anomalies are allowed to happen.
     */
    Serializable(false, false, false);

    private final boolean writeSkewAllowed;
    private final boolean dirtyReadAllowed;
    private final boolean inconsistentReadAllowed;

    IsolationLevel(boolean dirtyReadAllowed, boolean inconsistentReadAllowed, boolean writeSkewAllowed) {
        this.dirtyReadAllowed = dirtyReadAllowed;
        this.inconsistentReadAllowed = inconsistentReadAllowed;
        this.writeSkewAllowed = writeSkewAllowed;
    }

    /**
     * Checks if the writeskew is allowed to happen.
     *
     * @return true if the writeSkew is allowed to happen.
     */
    public final boolean isWriteSkewAllowed() {
        return writeSkewAllowed;
    }

    /**
     * Checks if the dirty read is allowed to happen (so reading data that has not been committed).
     *
     * @return true if the dirty read is allowed to happen.
     */
    public boolean isDirtyReadAllowed() {
        return dirtyReadAllowed;
    }

    /**
     * Checks if the inconsistent read is allowed to happen.
     *
     * @return true if the inconsistent read is allowed to happen.
     */
    public boolean isInconsistentReadAllowed() {
        return inconsistentReadAllowed;
    }

    @Override
    public String toString() {
        return "IsolationLevel."+name() + "{" +
                "writeSkewAllowed=" + writeSkewAllowed +
                ", dirtyReadAllowed=" + dirtyReadAllowed +
                ", inconsistentReadAllowed=" + inconsistentReadAllowed +
                '}';
    }
}
