package org.multiverse.api;

/**
 * The main interface for software transactional memory. Updates/reads in the stm should only be done through the {@link
 * Transaction} interface. So see that for more details.
 * <p/>
 * This interface is made for a shared clock based stm's. When in the future the shared clock is dropped, this interface
 * needs to be refactored or a new framework needs to be created.
 * <p/>
 * It is important that an TransactionalObject only is used within a single stm. If it is 'shared' between different
 * stm's, isolation problems could start to appear.
 *
 * @author Peter Veentjer.
 */
public interface Stm<B extends TransactionFactoryBuilder> {

    /**
     * Gets the {@link TransactionFactoryBuilder} that needs to be used to execute transactions on this Stm. See the {@link
     * TransactionFactoryBuilder} for more info.
     *
     * @return the TransactionFactoryBuilder that belongs to this Stm.
     */
    B getTransactionFactoryBuilder();

    /**
     * Returns the current clock version (this is logical time). The returned value will always be equal or larger than
     * zero. 
     * <p/>
     * This method is useful for stm's based on a central clock, but once this has been removed (a shared clock causes
     * contention and therefor limits scalability) this method is going to be removed.
     *
     * @return the current clock version.
     */
    long getVersion();
}
