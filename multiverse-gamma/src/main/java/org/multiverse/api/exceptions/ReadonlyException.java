package org.multiverse.api.exceptions;

/**
 * A {@link IllegalTransactionStateException} that indicates that a write action is executed using
 * a readonly transaction.
 *
 * @author Peter Veentjer.
 */
public class ReadonlyException extends IllegalTransactionStateException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new ReadonlyException.
     */
    public ReadonlyException() {
    }

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
