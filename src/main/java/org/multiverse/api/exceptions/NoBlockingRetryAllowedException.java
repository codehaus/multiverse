package org.multiverse.api.exceptions;

/**
 * A {@link RetryException} that indicates that a retry is done while it isn't allowed
 * (because the transaction doesn't allow blocking transactions).
 *
 * @author Peter Veentjer.
 */
public class NoBlockingRetryAllowedException extends RetryException{

    public NoBlockingRetryAllowedException() {
    }

    public NoBlockingRetryAllowedException(String message) {
        super(message);
    }

    public NoBlockingRetryAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoBlockingRetryAllowedException(Throwable cause) {
        super(cause);
    }
}
