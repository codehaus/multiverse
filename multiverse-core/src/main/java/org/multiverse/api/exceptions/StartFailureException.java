package org.multiverse.api.exceptions;

/**
 * An {@link IllegalStateException} that is thrown when a {@link org.multiverse.api.TransactionFactoryBuilder} failed
 * to start a transaction.
 * <p/>
 * todo: better name
 *
 * @author Peter Veentjer
 */
public class StartFailureException extends IllegalStateException {

    public StartFailureException() {
    }

    public StartFailureException(String message) {
        super(message);
    }

    public StartFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public StartFailureException(Throwable cause) {
        super(cause);
    }
}
