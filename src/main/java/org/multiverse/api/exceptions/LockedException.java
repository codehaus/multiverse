package org.multiverse.api.exceptions;

/**
 * An {@link AtomicOperationException} that indicates that an operation was executed on an ensured or privatized
 * ref or transactional object.
 *
 * @author Peter Veentjer.
 */
public class LockedException extends AtomicOperationException {

    public LockedException() {
    }

    public LockedException(String message) {
        super(message);
    }

    public LockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockedException(Throwable cause) {
        super(cause);
    }
}
