package org.multiverse.api;

import org.multiverse.api.collections.TransactionalCollectionsFactory;
import org.multiverse.api.references.RefFactory;
import org.multiverse.api.references.RefFactoryBuilder;

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
     * Creates an OrElseBlock.
     *
     * @return the created OrElseBlock.
     */
    OrElseBlock createOrElseBlock();

    /**
     * Returns the default reference factory that can be used for easy and cheap access to a reference factory
     * instead of setting one up through the RefFactoryBuilder.
     *
     * @return the default RefFactory.
     */
    RefFactory getDefaultRefFactory();

    /**
     * Gets the {@link TransactionFactoryBuilder} that needs to be used to execute transactions on this Stm. See the
     * {@link TransactionFactoryBuilder} for more info.
     *
     * @return the TransactionFactoryBuilder that needs to be used to execute transactions on this Stm.
     */
    TransactionFactoryBuilder createTransactionFactoryBuilder();

    TransactionalCollectionsFactory getDefaultTransactionalCollectionFactory();

    /**
     * Gets the {@link org.multiverse.api.references.RefFactoryBuilder}.
     *
     * @return the RefFactoryBuilder.
     */
    RefFactoryBuilder getRefFactoryBuilder();
}
