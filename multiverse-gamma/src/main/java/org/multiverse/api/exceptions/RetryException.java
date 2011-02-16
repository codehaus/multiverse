package org.multiverse.api.exceptions;

/**
 * A {@link TransactionExecutionException} that is thrown when the retry fails. The {@link RetryError} is thrown
 * when a retry happens, but when a {@link org.multiverse.api.Transaction} is not allowed to block, or too many
 * retries have happened, subclasses of this exception are thrown.
 *
 * @author Peter Veentjer.
 */
public abstract class RetryException extends TransactionExecutionException {

    private static final long serialVersionUID = 0;

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
     * @param cause   the cause of the RetryException.
     */
    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
