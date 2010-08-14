package org.multiverse.api.exceptions;

/**
 * A {@link org.multiverse.api.exceptions.PropagationException} thrown when a transaction is found, but is not allowed.
 *
 * @author Peter Veentjer.
 */
public class NoTransactionAllowedException extends PropagationException {

    public NoTransactionAllowedException() {
    }

    public NoTransactionAllowedException(String s) {
        super(s);
    }

    public NoTransactionAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoTransactionAllowedException(Throwable cause) {
        super(cause);
    }
}
