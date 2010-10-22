package org.multiverse.api.exceptions;

/**
 * An {@link AtomicOperationException} that indicates that an operation was executed on an ensured or privatized
 * ref or transactional object.
 *
 * @author Peter Veentjer.
 */
public class LockedException extends AtomicOperationException {

    /**
     * Creates a new LockedException.
     */
    public LockedException() {
    }

    /**
     * Creates a new LockedException
     *
     * @param message the message
     */
    public LockedException(String message) {
        super(message);
    }

    /**
     * Creates a new LockedException.
     *
     * @param message the message
     * @param cause the cause
     */
    public LockedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new LockedException
     *
     * @param cause the cause
     */
    public LockedException(Throwable cause) {
        super(cause);
    }
}
