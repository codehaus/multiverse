package org.multiverse.api;

/**
 * With the IsolationLevel you have a way to provide declarative control to guarantee isolation between transactions.
 * The transaction is free to provide a higher isolation than the one specified.
 * <p/>
 * The dirty read isn't added since atm we already have an extremely cheap read using the atomicWeakGet on the
 * refs. Using the atomicWeakGet you have extremely cheap access to committed data.
 * <p/>
 * <h2>Unrepeatable Read</h2>
 * <p/>
 * <h2>Inconsistent Read</h2>
 * <p/>
 * <h2>Writeskew</h2>
 *
 * @author Peter Veentjer.
 * @see TransactionFactoryBuilder#setIsolationLevel(IsolationLevel)
 */
public enum IsolationLevel {

    /**
     * This isolation level doesn't allow for uncommitted data to be read, but you don't get any consistency
     * guarantees. It could be that the value read changes over time (although when readtracking is used this
     * problem won't happen that often). And no guarantees are made that the data you read is consistent.
     * <p/>
     * Using the ReadCommitted isolation level can be dangerous since the consistency of the data is not guaranteed.
     * This is even more true for and updating transaction since it could leave objects in an inconsistent state. So
     * use it very carefully.
     */
    ReadCommitted(true, true, true),

    /**
     * With the RepeatableRead isolation level you will always see committed data and next to that once a read
     * is done, this read is going to be repeatable (so you will see the same value every time).
     */
    RepeatableRead(false, true, true),

    /**
     * The default isolation level that allows for the writeskew problem but not for dirty or unrepeatable
     * or inconsistent reads.
     * <p/>
     * This is the 'serialized' isolation level provided by MVCC databases like Oracle/Postgresql
     * (although Postgresql 9 is going to provide a truly serialized isolation level) and MySQL with the InnoDb.
     * All data read. contains committed data and all data will be consistent.
     * <p/>
     * A transaction that is readonly, gets the same isolation behavior as the Serializable isolation level
     * since the writeskew problem can't occur (nothing can be written).
     */
    Snapshot(false, false, true),

    /**
     * Provides truly serialized transaction at the cost of reduced performance and concurrency. This is the highest
     * isolation level where no isolation anomalies are allowed to happen. So the writeSkew problem is not allowed to
     * happen.
     */
    Serializable(false, false, false);

    private final boolean writeSkewAllowed;
    private final boolean unrepeatableReadAllowed;
    private final boolean inconsistentReadAllowed;

    IsolationLevel(boolean unrepeatableReadAllowed, boolean inconsistentReadAllowed, boolean writeSkewAllowed) {
        this.unrepeatableReadAllowed = unrepeatableReadAllowed;
        this.inconsistentReadAllowed = inconsistentReadAllowed;
        this.writeSkewAllowed = writeSkewAllowed;
    }

    /**
     * Checks if the writeskew problem is allowed to happen.
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
    public boolean isUnrepeatableReadAllowed() {
        return unrepeatableReadAllowed;
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
        return "IsolationLevel." + name();
    }
}