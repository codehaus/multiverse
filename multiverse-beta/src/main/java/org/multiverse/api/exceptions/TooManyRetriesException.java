package org.multiverse.api.exceptions;

/**
 * A {@link RetryException} that is thrown when a transaction is retried too many times. Uncontrolled
 * retrying could lead to liveness problems like livelocks and starvation.
 *
 * @author Peter Veentjer.
 */
public class TooManyRetriesException extends RetryException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new TooManyRetriesException
     */
    public TooManyRetriesException() {
    }

    /**
     * Creates a new TooManyRetriesException with the provided message.
     *
     * @param message the message of the exception.
     */
    public TooManyRetriesException(String message) {
        super(message);
    }
}
