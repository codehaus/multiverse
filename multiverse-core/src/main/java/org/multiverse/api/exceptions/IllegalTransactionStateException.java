package org.multiverse.api.exceptions;

/**
 * An {@link IllegalStateException} that is thrown when an operations is executed on a
 * Transaction when it is not in a valid state for that operation.
 *
 * @author Peter Veentjer
 */
public class IllegalTransactionStateException extends IllegalStateException {

    public IllegalTransactionStateException() {
    }

    public IllegalTransactionStateException(Throwable cause) {
        super(cause);
    }

    public IllegalTransactionStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalTransactionStateException(String s) {
        super(s);
    }
}
