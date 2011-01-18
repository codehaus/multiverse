package org.multiverse.api.exceptions;

/**
 * An {@link IllegalTransactionStateException} that is thrown when a transaction is configured
 * as abort only and a prepare/commit happens.
 *
 * @author Peter Veentjer.
 */
public class ExplicitAbortException extends IllegalTransactionStateException {

    public ExplicitAbortException() {
    }

    public ExplicitAbortException(String message) {
        super(message);
    }

    public ExplicitAbortException(String message, Throwable cause) {
        super(message, cause);
    }
}
