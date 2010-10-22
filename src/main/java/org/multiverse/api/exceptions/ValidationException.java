package org.multiverse.api.exceptions;

/**
 * A {@link TransactionExecutionException} thrown when the validation to a transactional object fails.
 *
 * @author Peter Veentjer.
 */
public class ValidationException extends TransactionExecutionException {

    private static final long serialVersionUID = 0;
        
    /**
     * Creates a new ValidationException.
     */
    public ValidationException() {
    }

    /**
     * Creates a new ValidationException with the provided message.
     *
     * @param message the message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Creates a new ValidationException with the provided message.
     *
     * @param message the message
     * @param cause   the Throwable that caused this ValidationException.
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ValidationException with the provided cause.
     *
     * @param cause the Throwable that caused this ValidationException.
     */
    public ValidationException(Throwable cause) {
        super(cause);
    }
}
