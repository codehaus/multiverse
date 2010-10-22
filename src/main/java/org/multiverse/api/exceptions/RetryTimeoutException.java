package org.multiverse.api.exceptions;

/**
 * A {@link RetryException} thrown when a transaction times out while it blocks on a retry (so waits for an update).
 *
 * On a transaction the maximum timeout can be set. When it is set to a bound value (so smaller than Long.MAX_VALUE)
 * all retries that need to block the transaction (so wait till some write happened) will decrement the
 * remaining timeout. When the transaction eventually times out, this Exception is thrown.
 *
 * For more information see:
 * <ol>
 * <li>the remaining timeout: {@link org.multiverse.api.Transaction#getRemainingTimeoutNs()}</li>
 * <li>reading the configured timeout: {@link org.multiverse.api.TransactionConfiguration#getTimeoutNs()}.</li>
 * <li>configuring the timeout: {@link org.multiverse.api.TransactionFactoryBuilder#setTimeoutNs(long)}</li>
 * </ol>
 *
 * @author Peter Veentjer.
 */
public class RetryTimeoutException extends RetryException {

    /**
     * Creates a new RetryTimeoutException.
     *
     * @param message the message of the exception.
     */
    public RetryTimeoutException(String message) {
        super(message);
    }

    /**
     * Creates a new RetryTimeoutException.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception
     */
    public RetryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

