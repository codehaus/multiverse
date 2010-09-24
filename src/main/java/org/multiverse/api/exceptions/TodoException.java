package org.multiverse.api.exceptions;

/**
 * Exception that indicates that some part of the implementation is missing. Normally they should never be
 * thrown in releases.
 *
 * @author Peter Veentjer
 */
public class TodoException extends RuntimeException {

    /**
     * Creates a new TodoException.
     */
    public TodoException() {
    }

    /**
     * Creates a new TodoException.
     *
     * @param message the message of the exception
     */
    public TodoException(String message) {
        super(message);
    }
}
