package org.multiverse.api.exceptions;

/**
 * A RetryException is thrown when the retry fails.
 *
 * @author Peter Veentjer.
 */
public abstract class RetryException extends RuntimeException{

    /**
     * Creates a new RetryException.
     */
    public RetryException() {
    }

    /**
     * Creates a new RetryException with the provided message.
     *
     * @param message the message of the RetryException.
     */
    public RetryException(String message) {
        super(message);
    }

    /**
     * Creates a new RetryException with the provided message and cause.
     *
     * @param message the message of the RetryException.
     * @param cause the cause of the RetryException.
     */
    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new RetryException with the provided cause.
     *
     * @param cause the cause of the RetryException.
     */
    public RetryException(Throwable cause) {
        super(cause);
    }
}
