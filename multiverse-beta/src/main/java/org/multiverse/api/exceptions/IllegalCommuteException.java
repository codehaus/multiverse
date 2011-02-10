package org.multiverse.api.exceptions;

/**
 * An {@link TransactionExecutionException} that is thrown when transaction access is done
 * while a commuting function is being evaluated.
 */
public class IllegalCommuteException extends TransactionExecutionException {

    public IllegalCommuteException(String message) {
        super(message);
    }

    public IllegalCommuteException(String message, Throwable cause) {
        super(message, cause);
    }
}
