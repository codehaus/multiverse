package org.multiverse.api.exceptions;

/**
 * An {@link IllegalStateException} that indicates that an atomic operation failed because the resource
 * was locked.
 *
 * @author Peter Veentjer.
 */
public class AtomicOperationException extends IllegalStateException{

    public AtomicOperationException() {
    }

    public AtomicOperationException(String message) {
        super(message);
    }

    public AtomicOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AtomicOperationException(Throwable cause) {
        super(cause);
    }
}
