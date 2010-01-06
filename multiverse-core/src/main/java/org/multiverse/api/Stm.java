package org.multiverse.api;

/**
 * The main interface for software transactional memory. Updates/reads in the stm should only be done through the {@link
 * Transaction} interface. So see that for more details.
 * <p/>
 * Essentially the Stm is a transaction factory.
 * <p/>
 * This interface is made for a shared clock based stm's. When in the future the shared clock is dropped, this interface
 * needs to be refactored or a new framework needs to be created.
 * <p/>
 * It is important that an atomic object only is used within a single stm.
 *
 * @author Peter Veentjer.
 */
public interface Stm {

    /**
     * Returns the current clock time (this is logical time). The returned value will always be equal or larger than
     * zero.
     * <p/>
     * This method is useful for stm's based on a central clock, but once this has been removed (a shared clock causes
     * contention and therefor limits scalability) this method is going to be removed.
     *
     * @return the current clock version.
     */
    long getTime();

    /**
     * Starts a Transaction that can be used for updates. The family name should be a string that uniquely identifies
     * transactions that execute the same logic. Based on the family name the stm could do all kinds of optimizations
     * like returning transaction implementations optimized for this type of transaction.
     * <p/>
     * See the {@link org.multiverse.api.annotations.AtomicMethod} for more information.
     *
     * @param familyName the familyName of the Transaction.
     * @return the created Transaction.
     */
    Transaction startUpdateTransaction(String familyName);

    /**
     * Starts Transaction that only can be used for readonly access. Updates are not allowed while using this
     * transaction and will cause a {@link org.multiverse.api.exceptions.ReadonlyException} to be thrown.
     *
     * @param familyName the familyName of the Transaction.
     * @return the created readonly Transaction.
     */
    Transaction startReadOnlyTransaction(String familyName);
}
