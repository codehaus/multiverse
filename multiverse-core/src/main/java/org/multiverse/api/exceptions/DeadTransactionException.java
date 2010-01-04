package org.multiverse.api.exceptions;

/**
 * An {@link IllegalStateException} that indicates that an action is executed on a transaction
 * that is not active.
 *
 * @author Peter Veentjer.
 */
public class DeadTransactionException extends IllegalStateException {

    private static final long serialVersionUID = 0;

    public DeadTransactionException() {
    }

    public DeadTransactionException(String message) {
        super(message);
    }

    public DeadTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeadTransactionException(Throwable cause) {
        super(cause);
    }
}
