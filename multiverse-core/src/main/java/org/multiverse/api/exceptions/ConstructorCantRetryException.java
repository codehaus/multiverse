package org.multiverse.api.exceptions;

/**
 * A {@link org.multiverse.api.exceptions.TooManyRetriesException} that is thrown when a
 * constructor wants to retry but isn't able to do.
 *
 * @author Peter Veentjer
 */
public class ConstructorCantRetryException extends TooManyRetriesException {

    public ConstructorCantRetryException() {
    }

    public ConstructorCantRetryException(Throwable cause) {
        super(cause);
    }

    public ConstructorCantRetryException(String message) {
        super(message);
    }

    public ConstructorCantRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
