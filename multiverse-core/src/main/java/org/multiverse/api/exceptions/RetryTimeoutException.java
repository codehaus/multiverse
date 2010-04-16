package org.multiverse.api.exceptions;

/**
 * A {@link RuntimeException} that is thrown in case of a transaction timeout
 * on a blocking operation (using the retry primitive).
 *
 * @author Peter Veentjer
 */
public class RetryTimeoutException extends RuntimeException {

    public RetryTimeoutException(String msg) {
        super(msg);
    }

    public RetryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
