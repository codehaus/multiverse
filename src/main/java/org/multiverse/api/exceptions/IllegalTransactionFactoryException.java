package org.multiverse.api.exceptions;

/**
 * An  {@link IllegalStateException} thrown when a TransactionFactory can't be created because the configuration is
 * not correct.
 *
 * @author Peter Veentjer.
 */
public class IllegalTransactionFactoryException extends IllegalStateException {

    public IllegalTransactionFactoryException() {
    }

    public IllegalTransactionFactoryException(String s) {
        super(s);
    }

    public IllegalTransactionFactoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalTransactionFactoryException(Throwable cause) {
        super(cause);
    }
}
