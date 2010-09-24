package org.multiverse.api.exceptions;

/**
 * A {@link IllegalTransactionStateException} that is thrown when an operation is executed on a
 * transaction while the transaction is prepared.
 *
 * @author Peter Veentjer.
 */
public class PreparedTransactionException extends IllegalTransactionStateException {

    /**
     * Creates a new PreparedTransactionException.
     */
    public PreparedTransactionException() {
    }

    /**
     * Creates a new PreparedTransactionException with the provided message.
     *
     * @param message the message of the exception.
     */
    public PreparedTransactionException(String message) {
        super(message);
    }

    /**
     * Creates a new PreparedTransactionException with the provided message and cause.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public PreparedTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
