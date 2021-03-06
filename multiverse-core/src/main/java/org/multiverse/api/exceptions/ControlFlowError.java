package org.multiverse.api.exceptions;

/**
 * A {@link Error} that is used to regulate control flow inside multiverse, to be more specific
 * jump from code to the handler the transaction management logic (instrumented or
 * {@link SpeculativeConfigurationFailure}) so it doesn't indicate a
 * bad thing.
 * <p/>
 * Normally it would be a very bad thing to regulate control flow using an exception/error, but
 * to make seamless integration in the Java language possible, there is no better alternative.
 * So you should not catch these exceptions unless you really know what you are doing.
 * <p/>
 * It is an Error instead of a RuntimeException, to prevent users trying to catch the error
 * inside a try/catch(RuntimeException) block and consuming important events like a ReadConflict
 * or a WriteConflict. In most cases these events can be solved by retrying the transaction.
 *
 * @author Peter Veentjer
 */
public abstract class ControlFlowError extends Error {

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

    public abstract String getDescription();
}
