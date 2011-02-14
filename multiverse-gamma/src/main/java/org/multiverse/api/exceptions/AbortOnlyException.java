package org.multiverse.api.exceptions;

/**
 * An {@link IllegalTransactionStateException} that is thrown when a transaction is configured
 * as abort only and a prepare/commit is executed.
 *
 * @author Peter Veentjer.
 */
public class AbortOnlyException extends IllegalTransactionStateException {

    /**
     * Creates a new AbortOnlyException with the provided message.
     *
     * @param message the message.
     */
    public AbortOnlyException(String message) {
        super(message);
    }

    /**
     * Creates a new AbortOnlyException with the provided message and cause.
     *
     * @param message the message
     * @param cause the cause.
     */
    public AbortOnlyException(String message, Throwable cause) {
        super(message, cause);
    }
}
