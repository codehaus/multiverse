package org.multiverse.api.exceptions;

/**
 * A {@link TransactionExecutionException} thrown when a transaction
 * encounters a transactional object that belongs to a different STM than the STM is belongs to.
 *
 * @author Peter Veentjer.
 */
public class StmMismatchException extends TransactionExecutionException {

    /**
     * Creates a new StmMismatchException with the provided message.
     *
     * @param message the message
     */
    public StmMismatchException(String message) {
        super(message);
    }

    /**
     * Creates a new StmMismatchException with the provided message.
     *
     * @param message the message
     * @param cause the cause
     */
    public StmMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
