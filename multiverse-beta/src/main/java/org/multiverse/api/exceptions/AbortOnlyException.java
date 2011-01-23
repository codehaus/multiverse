package org.multiverse.api.exceptions;

/**
 * An {@link IllegalTransactionStateException} that is thrown when a transaction is configured
 * as abort only and a prepare/commit happens.
 *
 * @author Peter Veentjer.
 */
public class AbortOnlyException extends IllegalTransactionStateException {

    public AbortOnlyException() {
    }

    public AbortOnlyException(String message) {
        super(message);
    }

    public AbortOnlyException(String message, Throwable cause) {
        super(message, cause);
    }
}
