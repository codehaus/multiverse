package org.multiverse.api.exceptions;

/**
 * An {@link TransactionalExecutionException} that is thrown when an operations is executed on a
 * Transaction when it is not in a valid state for that operation.
 *
 * @author Peter Veentjer
 */
public class IllegalTransactionStateException extends TransactionalExecutionException {

    /**
     * Creates a new IllegalTransactionStateException.
     */
    public IllegalTransactionStateException() {
    }

    /**
     * Creates a new IllegalTransactionStateException with the provided message.
     *
     * @param message the message of the exception.
     */
    public IllegalTransactionStateException(String message) {
        super(message);
    }

    /**
     * Creates a new IllegalTransactionStateException with the provided message and cause.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public IllegalTransactionStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
