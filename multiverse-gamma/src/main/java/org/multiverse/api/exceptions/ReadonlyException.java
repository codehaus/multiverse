package org.multiverse.api.exceptions;

/**
 * An {@link IllegalTransactionStateException} thrown when a write action is executed using
 * a readonly {@link org.multiverse.api.Transaction}.
 *
 * @author Peter Veentjer.
 * @see org.multiverse.api.TransactionFactoryBuilder#setReadonly(boolean)
 */
public class ReadonlyException extends IllegalTransactionStateException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new ReadonlyException.
     *
     * @param message the message of the exception.
     */
    public ReadonlyException(String message) {
        super(message);
    }

    /**
     * Creates a new ReadonlyException.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public ReadonlyException(String message, Throwable cause) {
        super(message, cause);
    }

}
