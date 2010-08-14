package org.multiverse.api.exceptions;

/**
 * @author Peter Veentjer
 */
public class TodoException extends RuntimeException{

    public TodoException() {
    }

    public TodoException(String message) {
        super(message);
    }
}
