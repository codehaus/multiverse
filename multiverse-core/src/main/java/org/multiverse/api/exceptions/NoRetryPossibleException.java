package org.multiverse.api.exceptions;

/**
 * An {@link IllegalTransactionStateException} that indicates that a retry is done, without the
 * possibility of progress, for example when the readset is empty.
 * <p/>
 * No reason to create a singleton for performance reasons since this exception should not
 * occur. So if it does, we want a complete stacktrace.
 *
 * @author Peter Veentjer.
 */
public class NoRetryPossibleException extends IllegalTransactionStateException {

    private static final long serialVersionUID = 0;

    public NoRetryPossibleException() {
    }

    public NoRetryPossibleException(String message) {
        super(message);
    }

    public NoRetryPossibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoRetryPossibleException(Throwable cause) {
        super(cause);
    }
}

