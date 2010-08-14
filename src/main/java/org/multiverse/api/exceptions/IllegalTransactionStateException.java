package org.multiverse.api.exceptions;

/**
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
