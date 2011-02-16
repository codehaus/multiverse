package org.multiverse.api;

/**
 * A Factory responsible for creating a {@link Transaction}. To set properties on Transactions you need to look
 * at the {@link TransactionFactoryBuilder}.
 *
 * <h3>Thread safety</h3>
 *
 * <p>A TransactionFactory is thread-safe and it is expected to be shared between threads (doesn't impose it, but it
 * is the most logical use case). It also is expected to be re-used instead of recreated.
 *
 * @author Peter Veentjer.
 */
public interface TransactionFactory {

    /**
     * Gets the TransactionConfiguration used by this TransactionFactory.
     *
     * @return the TransactionConfiguration.
     */
    TransactionConfiguration getConfiguration();

    /**
     * Creates a new transaction.
     *
     * @return the created Transaction.
     */
    Transaction newTransaction();
}
