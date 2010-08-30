package org.multiverse.api;

import org.multiverse.api.references.ReferenceFactoryBuilder;

/**
 * The main interface for software transactional memory. Updates/reads in the stm should only be done through the
 * {@link Transaction}. So see that for more details.
 * <p/>
 * It is important that an TransactionalObject only is used within a single stm. If it is 'shared' between different
 * stm instances, isolation problems could happen. This can be caused by the fact that different stm instances
 * probably use different clocks or completely different mechanisms for preventing isolation problems.
 * <p/>
 * All methods on the Stm are of course thread safe.
 *
 * @author Peter Veentjer.
 */
public interface Stm {

    /**
     * Starts a default Transaction that is useful for testing/experimentation purposes. This method is purely
     * for easy to use access, but doesn't provide any configuration options. See the
     * {@link #createTransactionFactoryBuilder()} for something more configurable.
     * <p/>
     * Transactions returned by this method are not speculative.
     *
     * @return the started default Transaction.
     */    
    Transaction startDefaultTransaction();

    /**
     * Returns the default atomic block that is useful for testing/experimentation purposes. This method is purely
     * for easy to use access, but it doesn't provide any configuration options. See the
     * {@link #createTransactionFactoryBuilder()} for something more configurable.
     * <p/>
     * Transactions used in this Block are not speculative.
     *
     * @return the default AtomicBlock.
     */
    AtomicBlock getDefaultAtomicBlock();

    /**
     * Gets the {@link TransactionFactoryBuilder} that needs to be used to execute transactions on this Stm. See the
     * {@link TransactionFactoryBuilder} for more info.
     *
     * @return the TransactionFactoryBuilder that needs to be used to execute transactions on this Stm.
     */
    TransactionFactoryBuilder createTransactionFactoryBuilder();

    /**
     * Gets the {@link org.multiverse.api.references.ReferenceFactoryBuilder}.
     *
     * @return the ReferenceFactoryBuilder.
     */
    ReferenceFactoryBuilder getReferenceFactoryBuilder();
}
