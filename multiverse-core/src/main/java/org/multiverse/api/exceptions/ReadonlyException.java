package org.multiverse.api.exceptions;

/**
 * A {@link IllegalTransactionStateException} that indicates that an action is executed on a
 * readonly transaction that requires an update.
 *
 * @author Peter Veentjer.
 */
public class ReadonlyException extends IllegalTransactionStateException {

    private static final long serialVersionUID = 0;

    public ReadonlyException() {
    }

    public ReadonlyException(String message) {
        super(message);
    }

    public ReadonlyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadonlyException(Throwable cause) {
        super(cause);
    }
}
