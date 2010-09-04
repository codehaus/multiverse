package org.multiverse.api;

/**
 *
 * @author Peter Veentjer.
 */
public interface TransactionalObject {

    /**
     * Returns the Stm this TransactionalObject is part of.
     *
     * @return the Stm this TransactionalObject is part of.
     */
    Stm getStm();

    /**
     * Gets the LockStatus.
     *
     * @param tx
     * @return
     */
    LockStatus getLockStatus(Transaction tx);
}
