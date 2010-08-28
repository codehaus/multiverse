package org.multiverse.api;

/**
 * A Factory responsible for creating a {@link Transaction}. To set properties for Transactions you need to look
 * at the {@link TransactionFactoryBuilder}.
 * <p/>
 * A TransactionFactory is thread-safe and it is expected to be shared between threads (doesn't impose it, but it
 * is the most logical use case). It also is expected to be re-used instead of recreated.
 */
public interface TransactionFactory {

    /**
     * Gets the TransactionConfiguration used by this TransactionFactory.
     *
     * @return the TransactionConfiguration.
     */
    TransactionConfiguration getTransactionConfiguration();

    /**
     * Starts a transaction.
     *
     * @return the started Transaction.
     */
    Transaction start();
}
