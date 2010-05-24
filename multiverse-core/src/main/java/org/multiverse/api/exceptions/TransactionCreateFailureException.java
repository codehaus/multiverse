package org.multiverse.api.exceptions;

/**
 * An {@link IllegalStateException} that is thrown when a {@link org.multiverse.api.TransactionFactoryBuilder} failed
 * to start a transaction.
 * <p/>
 * todo: better name
 *
 * @author Peter Veentjer
 */
public class TransactionCreateFailureException extends IllegalStateException {

    public TransactionCreateFailureException() {
    }

    public TransactionCreateFailureException(String message) {
        super(message);
    }

    public TransactionCreateFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionCreateFailureException(Throwable cause) {
        super(cause);
    }
}
