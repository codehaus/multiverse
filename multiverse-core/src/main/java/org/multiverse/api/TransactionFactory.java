package org.multiverse.api;

/**
 * A Factory responsible for creating a {@link Transaction}. To set properties for Transactions you need to look
 * at the {@link TransactionFactoryBuilder}.
 * <p/>
 * A TransactionFactory is thread-safe and it is expected to be shared between threads (doesn't impose it, but it
 * is the most logical use case). It also is expected to be re-used instead of recreated.
 *
 * @param <T>
 */
public interface TransactionFactory<T extends Transaction> {

    /**
     * Returns the TransactionFactoryBuilder that created this TransactionFactory.
     *
     * @return the TransactionFactoryBuilder that created this TransactionFactory.
     */
    TransactionFactoryBuilder getTransactionFactoryBuilder();

    /**
     * Returns the Stm this TransactionFactory belongs to.
     *
     * @return the Stm this TransactionFactory belongs to.
     */
    Stm getStm();

    /**
     * Creates a new unstarted Transaction.
     *
     * @return the created Transaction.
     * @throws org.multiverse.api.exceptions.TransactionCreateFailureException
     *          if creating a new transaction
     *          failed.
     */
    T create();

    /**
     * Creates a new and started Transaction.
     *
     * @return the created Transaction
     * @throws org.multiverse.api.exceptions.TransactionCreateFailureException
     *          if starting a new transaction failed.
     */
    T start();
}
