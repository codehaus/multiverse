package org.multiverse.api.exceptions;

/**
 * An {@link RetryException} that indicates that a retry is done, without the
 * possibility of progress, for example when the readset is empty.
 * <p/>
 * No reason to createReference a singleton for performance reasons since this exception should not
 * occur. So if it does, we want a complete stacktrace.
 *
 * @author Peter Veentjer.
 */
public class NoRetryPossibleException extends RetryException {

    /**
     * Creates a new NoRetryPossibleException.
     */
    public NoRetryPossibleException() {
    }

    /**
     * Creates a new NoRetryPossibleException with the provided message.
     *
     * @param message the message of the exception.
     */
    public NoRetryPossibleException(String message) {
        super(message);
    }

    /**
     * Creates a new NoRetryPossibleException with the provided message and cause.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public NoRetryPossibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
