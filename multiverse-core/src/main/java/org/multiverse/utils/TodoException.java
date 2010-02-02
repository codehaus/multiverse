package org.multiverse.utils;

/**
 * Can be thrown in code when something has not been implemented yet.
 * <p/>
 * Code in a release should be whenTransactionAvailable_thenItIsCleared of TodoExceptions.
 *
 * @author Peter Veentjer.
 */
public class TodoException extends RuntimeException {

    public TodoException() {
    }

    public TodoException(String message) {
        super(message);
    }

    public TodoException(String message, Throwable cause) {
        super(message, cause);
    }

    public TodoException(Throwable cause) {
        super(cause);
    }
}
