package org.multiverse.api.exceptions;

/**
 * An {@link Error} that is thrown if the system has entered an invalid state. Normally this
 * should never happen.
 *
 * @author Peter Veentjer.
 */
public class PanicError extends Error {

    private static final long serialVersionUID = 0;

    public PanicError() {
    }

    public PanicError(String message) {
        super(message);
    }

    public PanicError(String message, Throwable cause) {
        super(message, cause);
    }

    public PanicError(Throwable cause) {
        super(cause);
    }
}
