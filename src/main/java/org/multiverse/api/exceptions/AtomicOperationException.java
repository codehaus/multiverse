package org.multiverse.api.exceptions;

/**
 * An {@link TransactionalExecutionException} that indicates that an atomic operation so one with
 *
 * @author Peter Veentjer.
 */
public class AtomicOperationException extends TransactionalExecutionException {

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
     * Creates a new Atomic
     * @param message
     * @param cause
     */
    public AtomicOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AtomicOperationException(Throwable cause) {
        super(cause);
    }
}
