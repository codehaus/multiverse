package org.multiverse.api.exceptions;

/**
 * A {@link Error} that is used to regulate control flow inside multiverse. Normally it would be a very bad thing to regulate
 * control flow using an exception/error, but to make seamless integration in the Java language possible, there is no better
 * alternative. So these exceptions should not catch unless you really know know what you are doing. So catching all Throwable
 * instances (including Error) is a bad practice.
 * <p/>
 * There current are 3 different types of ControlFlowErrors:
 * <ol>
 * <li>{@link ReadWriteConflict}: used for blocking</li>
 * <li>{@link RetryError}: used for blocking</li>
 * <li>{@link SpeculativeConfigurationError}: used for guessing the most optimal configuration for transactions</li>
 * </ol>
 * <h2>Why it is an Error</h2>
 * It is an Error instead of a RuntimeException, to prevent users trying to catch the error
 * inside a try/catch(RuntimeException) block and consuming important events like a ReadWriteConflict.
 * In most cases these events can be solved by retrying the transaction.
 * <p/>
 * It is an Error instead of a Throwable, because a Throwable needs to be propagated, so making the code a lot more
 * awkward to work with.
 * <h2>Instance Caching</h2>
 * Normally ControlFlowErrors are cached to be reused because they can be thrown very often to be caught by the AtomicBlock
 * and discarded. Especially the stacktrace is very expensive to create. By default all ControlFlowErrors are reused
 * but with the {@link org.multiverse.api.TransactionFactoryBuilder#setControlFlowErrorsReused(boolean)} this behavior
 * can be changed. It also can be configured on the Stm level, depending on the Stm implementation. For the
 * {@link org.multiverse.stms.gamma.GammaStm} you need to look at the
 * {@link org.multiverse.stms.gamma.GammaStmConfiguration#controlFlowErrorsReused}.
 * <p/>
 * The constructors also expose configuration options to have the StackTrace filled. Especially for an Error that is
 * reused, not filling the stacktrace is very important because else the developer is looking at code where the exception
 * very likely didn't happen.
 *
 * @author Peter Veentjer
 */
public abstract class ControlFlowError extends Error {

    private static final long serialVersionUID = 0;

    private final boolean fillStackTrace;

    /**
     * Creates a new ControlFlowError.
     *
     * @param fillStackTrace true if the StackTrace should be filled, false otherwise.
     */
    public ControlFlowError(boolean fillStackTrace) {
        this(fillStackTrace, null, null);
    }

    /**
     * Creates a new ControlFlowError with the provided message.
     *
     * @param fillStackTrace true if the StackTrace should be filled, false otherwise.
     * @param message        the message of the exception.
     */
    public ControlFlowError(boolean fillStackTrace, String message) {
        this(fillStackTrace, message, null);
    }

    /**
     * Creates a new ControlFlowError with the provided message and cause.
     *
     * @param fillStackTrace true if the StackTrace should be filled, false otherwise.
     * @param message        the message of the exception.
     * @param cause          the cause of the exception.
     */
    public ControlFlowError(boolean fillStackTrace, String message, Throwable cause) {
        super(message, cause);
        this.fillStackTrace = fillStackTrace;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        if (fillStackTrace) {
            return super.getStackTrace();
        } else {
            return new StackTraceElement[0];
        }
    }
}
