package org.multiverse.api.exceptions;

/**
 * An {@link PropagationException} that can be thrown when no transaction but if it was expected. A typical
 * cause of this exception is that the PropagationLevel.Mandatory is used.
 *
 * @author Peter Veentjer
 */
public class TransactionRequiredException extends PropagationException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new TransactionRequiredException.
     */
    public TransactionRequiredException() {
    }

    /**
     * Creates a new TransactionRequiredException with the provided message.
     *
     * @param message the message of the exception.
     */
    public TransactionRequiredException(String message) {
        super(message);
    }

    /**
     * Creates a new TransactionRequiredException with the provided message.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public TransactionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
