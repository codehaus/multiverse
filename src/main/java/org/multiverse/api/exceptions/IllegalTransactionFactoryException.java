package org.multiverse.api.exceptions;

/**
 * An  {@link IllegalStateException} thrown when a TransactionFactory can't be created because the configuration is
 * not correct.
 *
 * @author Peter Veentjer.
 */
public class IllegalTransactionFactoryException extends IllegalStateException {

    /**
     * Creates a new IllegalTransactionFactoryException.
     *
     * @param message the message of the IllegalTransactionFactoryException.
     */
    public IllegalTransactionFactoryException(String message) {
        super(message);
    }

    /**
     * Creates a new IllegalTransactionFactoryException with the provided message and cause.
     *
     * @param message the message of the IllegalTransactionFactoryException.
     * @param cause   the cause of the IllegalTransactionFactoryException
     */
    public IllegalTransactionFactoryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new IllegalTransactionFactoryException.
     *
     * @param cause the cause of the exception.
     */
    public IllegalTransactionFactoryException(Throwable cause) {
        super(cause);
    }
}
