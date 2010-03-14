package org.multiverse.api.exceptions;

/**
 * A {@link Error} that is used to regulate control flow, so it doesn't indicate a bad thing.
 * <p/>
 * Normally it would be a very bad thing to regulate control flow using an exception/error, but
 * to make seamless integration in the Java language possible, there is no better alternative.
 * <p/>
 * <p/>
 * It is an Error to prevent users trying to catch the error inside a try/catch(RuntimeException)
 * block and consuming important events like a ReadConflict or a WriteConflict.
 *
 * @author Peter Veentjer
 */
public class ControlFlowError extends Error {

    public ControlFlowError() {
    }

    public ControlFlowError(Throwable cause) {
        super(cause);
    }

    public ControlFlowError(String message) {
        super(message);
    }

    public ControlFlowError(String message, Throwable cause) {
        super(message, cause);
    }
}
