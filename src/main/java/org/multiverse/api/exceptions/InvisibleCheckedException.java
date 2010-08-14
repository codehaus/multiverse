package org.multiverse.api.exceptions;

/**
 * An {@link RuntimeException} that wraps a checked exception. It is useful if a checked exception
 * is thrown, but can't be rethrown.
 *
 * @author Peter Veentjer
 */
public class InvisibleCheckedException extends RuntimeException {

    static final long serialVersionUID = 0;

    public InvisibleCheckedException(Exception cause) {
        super(cause);
    }

    @Override
    public Exception getCause() {
        return (Exception) super.getCause();
    }
}
