package org.multiverse.api;

/**
 * The main interface for software transactional memory. Updates/reads in the stm should only be done through the
 * {@link Transaction}. So see that for more details.
 * <p/>
 * It is important that an TransactionalObject only is used within a single stm. If it is 'shared' between different
 * stm's, isolation problems could happen. This can be caused by the fact that different stm's probably use
 * different clocks. And the clock is needed to prevent isolation problems.
 * <p/>
 * All methods on the Stm are of course thread safe.
 *
 * @author Peter Veentjer.
 */
public interface Stm<B extends TransactionFactoryBuilder, P extends ProgrammaticReferenceFactoryBuilder> {

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

    /**
     * Gets the {@link TransactionFactoryBuilder} that needs to be used to execute transactions on this Stm. See the
     * {@link TransactionFactoryBuilder} for more info.
     *
     * @return the TransactionFactoryBuilder that needs to be used to execute transactions on this Stm.
     */
    B getTransactionFactoryBuilder();

    /**
     * Returns the programmatic reference factory this Stm exposes. See the
     * {@link org.multiverse.api.ProgrammaticReference} for more information when to use it.
     *
     * @return the used ProgrammaticReferenceFactory.
     */
    P getProgrammaticReferenceFactoryBuilder();
}
