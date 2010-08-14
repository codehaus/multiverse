package org.multiverse.api.exceptions;

/**
 * An {@link IllegalStateException} throws when there is a conflict with the Transaction propagation. For more information
 * {@link org.multiverse.api.PropagationLevel}.
 *
 * @author Peter Veentjer.
 */
public class PropagationException extends IllegalStateException {
    public PropagationException() {
    }

    public PropagationException(String s) {
        super(s);
    }

    public PropagationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropagationException(Throwable cause) {
        super(cause);
    }
}
