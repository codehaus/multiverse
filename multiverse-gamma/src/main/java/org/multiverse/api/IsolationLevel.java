package org.multiverse.api;

/**
 * With the IsolationLevel you have a way to provide declarative control to guarantee isolation between transactions.
 * The transaction is free to provide a higher isolation than the one specified.
 * <p/>
 * The dirty read isn't added since atm we already have an extremely cheap read using the atomicWeakGet on the
 * refs. Using the atomicWeakGet you have extremely cheap access to committed data.
 *
 * <p>The following isolation anomalies have been identified:
 * <ol>
 *     <li>Dirty Read</li>
 *     <li>Unrepeatable Read</li>
 *     <li>Inconsistent Read</li>
 *     <li>Write Skew</li>
 * </ol>
 *
 * <h3>Dirty Read</h3>
 *
 * <p>The DirtyRead isolation anomaly is that one transaction could observe uncommitted changes made by another transaction.
 * The biggest problem with this anomaly is that eventually the updating transaction could abort and the reading transaction
 * sees changes that never made it into the system.
 *
 * <p>Currently the Dirty Read anomaly is not possible in Multiverse since writes to main memory are deferred until commit
 * and once the first write to main memory is executed, the following writes are guaranteed to succeed.
 *
 * <h3>Unrepeatable Read</h3>
 *
 * <p>The Unrepeatable Read isolation anomaly is that a transaction could see changes made by other transactions. So at one
 * moment it could see value X and a moment later it could see Y. This could lead to all kinds of erratic behavior like
 * transaction that can get stuck in an infinitive loop.
 *
 * <p>Such a transaction is called a zombie transaction and can cause serious damage since they are consuming resources
 * (like cpu) and are holding on to various resources (like Locks).
 *
 * <h3>Inconsistent Read</h3>
 *
 * <p>With the inconsistent read isolation level
 *
 * <h3>Writeskew</h3>
 *
 * <h3>Isolation Levels</h3>
 *
 * <table>
 *
 * </table>
 *
 *
 * <h3>Implementation and isolation level upgrade</h3>
 *
 * <p>An implementation of the {@link Transaction} is free to upgrade the isolation level to a higher one if it doesn't support that specific isolation
 * level. This is the same as Oracle is doing with the ReadUncommitted, which automatically is upgraded to a ReadCommitted or the RepeatableRead which is
 * automatically upgraded to Snapshot (Oracle calls this the Serialized isolation level).
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

    private final boolean allowWriteSkew;
    private final boolean allowUnrepeatableRead;
    private final boolean allowInconsistentRead;

    IsolationLevel(boolean allowUnrepeatableRead, boolean allowInconsistentRead, boolean allowWriteSkew) {
        this.allowUnrepeatableRead = allowUnrepeatableRead;
        this.allowInconsistentRead = allowInconsistentRead;
        this.allowWriteSkew = allowWriteSkew;
    }

    /**
     * Checks if the writeskew problem is allowed to happen.
     *
     * @return true if the writeSkew is allowed to happen.
     */
    public final boolean doesAllowWriteSkew() {
        return allowWriteSkew;
    }

    /**
     * Checks if the dirty read is allowed to happen (so reading data that has not been committed).
     *
     * @return true if the dirty read is allowed to happen.
     */
    public boolean doesAllowUnrepeatableRead() {
        return allowUnrepeatableRead;
    }

    /**
     * Checks if the inconsistent read is allowed to happen.
     *
     * @return true if the inconsistent read is allowed to happen.
     */
    public boolean doesAllowInconsistentRead() {
        return allowInconsistentRead;
    }

    @Override
    public String toString() {
        return "IsolationLevel." + name();
    }
}
