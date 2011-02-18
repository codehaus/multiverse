package org.multiverse.api.exceptions;

/**
 * A {@link TransactionExecutionException} that is thrown when {@link org.multiverse.api.Transaction} access is done while
 * a commuting function is being evaluated.
 *
 * <p>The reason why Transaction access is not allowed,  is that once other reads/writes are done while executing the commuting
 * behavior, you can have read/write inconsistencies. E.g. in Clojure the same commuting function can be executed more than
 * once on a reference, leading to different values every time executed (e.g. the value it already had inside the transaction,
 * and the most recent committed value when the commuting operation is calculated during transaction commit.
 *
 * @author Peter Veentjer.
 */
public class IllegalCommuteException extends TransactionExecutionException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new IllegalCommuteException with the provided message.
     *
     * @param message the message
     */
    public IllegalCommuteException(String message) {
        super(message);
    }

    /**
     * Creates a new IllegalCommuteException with the provided message and cause.
     *
     * @param message the message
     * @param cause   the cause.
     */
    public IllegalCommuteException(String message, Throwable cause) {
        super(message, cause);
    }
}
