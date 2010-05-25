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

    TransactionFactoryBuilder getBuilder();

    /**
     * Creates a new and started Transaction.
     * <p/>
     * If in the future unstarted Transactions are wanted, contact one of the Multiverse developers.
     *
     * @return the created Transaction
     * @throws org.multiverse.api.exceptions.StartFailureException
     *          if starting a new transaction failed.
     */
    T start();
}
