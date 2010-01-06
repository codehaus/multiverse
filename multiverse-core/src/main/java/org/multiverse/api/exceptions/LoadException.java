package org.multiverse.api.exceptions;

/**
 * A {@link RuntimeException} that indicates that a load action on the transaction failed.
 *
 * @author Peter Veentjer.
 */
public abstract class LoadException extends RuntimeException {

    private static final long serialVersionUID = 0;

    public LoadException() {
    }

    public LoadException(String message) {
        super(message);
    }

    public LoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadException(Throwable cause) {
        super(cause);
    }
}
