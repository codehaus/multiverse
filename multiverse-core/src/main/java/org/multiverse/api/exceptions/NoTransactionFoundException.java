package org.multiverse.api.exceptions;

/**
 * An exception that can be thrown when no transaction but if it was expected.
 *
 * @author Peter Veentjer
 */
public class NoTransactionFoundException extends IllegalStateException {

    private static final long serialVersionUID = 0;

    public NoTransactionFoundException() {
    }

    public NoTransactionFoundException(Throwable cause) {
        super(cause);
    }

    public NoTransactionFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoTransactionFoundException(String s) {
        super(s);
    }
}
