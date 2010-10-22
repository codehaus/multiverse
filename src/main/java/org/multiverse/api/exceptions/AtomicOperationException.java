package org.multiverse.api.exceptions;

/**
 * An {@link TransactionExecutionException} that indicates that an atomic operation so one with
 *
 * @author Peter Veentjer.
 */
public class AtomicOperationException extends TransactionExecutionException {

    /**
     * Creates a new AtomicOperationException.
     */
    public AtomicOperationException() {
    }

    /**
     * Creates a new AtomicOperationException with the provided message.
     *
     * @param message the message
     */
    public AtomicOperationException(String message) {
        super(message);
    }

    /**
     * Creates a new AtomicOperationException with the provided message and cause.
     *
     * @param message  the message
     * @param cause the cause of the message
     */
    public AtomicOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new AtomicOperationException with the provided message and cause.
     *
     * @param cause the cause of the exception.
     */
    public AtomicOperationException(Throwable cause) {
        super(cause);
    }
}
