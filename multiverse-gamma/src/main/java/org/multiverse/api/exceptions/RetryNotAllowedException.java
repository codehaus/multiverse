package org.multiverse.api.exceptions;

/**
 * A {@link RetryException} that indicates that a retry is done while it isn't allowed
 * (because the transaction doesn't allow blocking transactions).
 * <p/>
 * For more information see {@link org.multiverse.api.TransactionFactoryBuilder#setBlockingAllowed(boolean)}
 * and {@link org.multiverse.api.TransactionConfiguration#isBlockingAllowed()}.
 *
 * @author Peter Veentjer.
 */
public class RetryNotAllowedException extends RetryException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new RetryNotAllowedException with the provided message.
     *
     * @param message the message
     */
    public RetryNotAllowedException(String message) {
        super(message);
    }

    /**
     * Creates a new RetryNotAllowedException with the provided message and cause.
     *
     * @param message the message
     * @param cause   the cause of this exception.
     */
    public RetryNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
