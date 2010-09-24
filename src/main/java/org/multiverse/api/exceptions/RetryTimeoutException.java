package org.multiverse.api.exceptions;

/**
 * A {@link RetryException} thrown when a transaction times out when it does a retry.
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

