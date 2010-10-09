package org.multiverse.api.exceptions;

/**
 * @author Peter Veentjer.
 */
public class TransactionalExecutionException extends RuntimeException {

    /**
     * Creates a new TransactionalExecutionException.
     */
    public TransactionalExecutionException() {
        super();
    }

    /**
     * Creates a new TransactionalExecutionException with the provided message and cause.
     *
     * @param message message of the exception.
     */
    public TransactionalExecutionException(String message) {
        super(message);
    }

    /**
     * Creates a new TransactionalExecutionException with the provided message and cause.
     *
     * @param message the message of the exception.
     * @param cause   the Throwable that caused the exception.
     */
    public TransactionalExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new TransactionalExecutionException with the provided cause.
     *
     * @param cause the Throwable that was the cause of this TransactionalExecutionException.
     */
    public TransactionalExecutionException(Throwable cause) {
        super(cause);
    }
}
