package org.multiverse.api.exceptions;

/**
 * A {@link IllegalTransactionStateException} that is thrown when an operation is executed on a
 * transaction while the transaction is prepared.
 *
 * @author Peter Veentjer.
 */
public class PreparedTransactionException extends IllegalTransactionStateException {

    public PreparedTransactionException() {
    }

    public PreparedTransactionException(String s) {
        super(s);
    }

    public PreparedTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreparedTransactionException(Throwable cause) {
        super(cause);
    }
}
