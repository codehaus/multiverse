package org.multiverse.api.exceptions;

/**
 * @author Peter Veentjer
 */
public class PreparedTransactionException extends IllegalTransactionStateException{

    public PreparedTransactionException() {
    }

    public PreparedTransactionException(Throwable cause) {
        super(cause);
    }

    public PreparedTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreparedTransactionException(String s) {
        super(s);
    }
}
