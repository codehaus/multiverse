package org.multiverse.api;

/**
 * The main interface for software transactional memory. Updates/reads in the stm should only be done through the
 * {@link Transaction}. So see that for more details.
 * <p/>
 * It is important that an TransactionalObject only is used within a single stm. If it is 'shared' between different
 * stm's, isolation problems could happen. This can be caused by the fact that different stm's probably use
 * different clocks or completely different mechanisms for preventing isolation problems.
 * <p/>
 * All methods on the Stm are of course thread safe.
 *
 * @author Peter Veentjer.
 */
public interface Stm {

    /**
     * Gets the {@link TransactionFactoryBuilder} that needs to be used to execute transactions on this Stm. See the
     * {@link TransactionFactoryBuilder} for more info.
     *
     * @return the TransactionFactoryBuilder that needs to be used to execute transactions on this Stm.
     */
    TransactionFactoryBuilder getTransactionFactoryBuilder();
}
