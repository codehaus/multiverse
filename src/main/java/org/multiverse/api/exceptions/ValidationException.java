package org.multiverse.api.exceptions;

/**
 * A {@link RuntimeException} thrown when the validation to a transactional object fails.
 *
 * @author Peter Veentjer.
 */
public class ValidationException extends RuntimeException{

    public ValidationException() {
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }
}
