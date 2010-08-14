package org.multiverse.api.exceptions;

/**
 * @author Peter Veentjer
 */
public class TooManyRetriesException extends RuntimeException {

    public TooManyRetriesException() {
    }

    public TooManyRetriesException(String message) {
        super(message);
    }
}
