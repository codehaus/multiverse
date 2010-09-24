package org.multiverse.api.exceptions;

/**
 * An {@link IllegalStateException} that can be thrown when no transaction but if it was expected. A typical
 * cause of this exception is that the PropagationLevel.Mandatory is used.
 *
 * @author Peter Veentjer
 */
public class NoTransactionFoundException extends IllegalStateException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new NoTransactionFoundException.
     */
    public NoTransactionFoundException() {
    }

    /**
     * Creates a new NoTransactionFoundException with the provided message.
     *
     * @param message the message of the exception.
     */
    public NoTransactionFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new NoTransactionFoundException with the provided message.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public NoTransactionFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
