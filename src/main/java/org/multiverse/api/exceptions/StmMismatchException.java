package org.multiverse.api.exceptions;

/**
 * A {@link org.multiverse.api.exceptions.TransactionalExecutionException} thrown when a transaction
 * encounters a transactional object that belongs to a different STM than the STM is belongs to.
 *
 * @author Peter Veentjer.
 */
public class StmMismatchException extends TransactionalExecutionException{

    public StmMismatchException(String message) {
        super(message);
    }

    public StmMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
