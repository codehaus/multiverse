package org.multiverse.api.exceptions;

/**
 * A {@link IllegalTransactionStateException} that indicates that an action is executed on a
 * readonly transaction that requires an update.
 *
 * @author Peter Veentjer.
 */
public class ReadonlyException extends IllegalTransactionStateException {

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
