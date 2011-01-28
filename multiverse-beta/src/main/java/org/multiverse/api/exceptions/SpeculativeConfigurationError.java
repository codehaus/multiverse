package org.multiverse.api.exceptions;

/**
 * A {@link ControlFlowError} that indicates that current transaction implementation can't deal
 * with more transactional objects than it can handle. This Error is useful for the STM
 * to speculative selection of a better performing implementation. So it can start with a very
 * fast transaction that only is able to deal with one or a few transactional objects and it
 * able to grow to more advanced but slower transaction implementations
 *
 * @author Peter Veentjer.
 */
public class SpeculativeConfigurationError extends ControlFlowError {

    private static final long serialVersionUID = 0;

    public final static SpeculativeConfigurationError INSTANCE = new SpeculativeConfigurationError(false);

    private final boolean fillStackTrace;

    /**
     * Creates a SpeculativeConfigurationError.
     */
    public SpeculativeConfigurationError(boolean fillStackTrace) {
        this.fillStackTrace = fillStackTrace;
    }

    /**
     * Creates a SpeculativeConfigurationError with the provided message.
     *
     * @param message the message of the exception.
     */
    public SpeculativeConfigurationError(String message) {
        super(message);
        this.fillStackTrace = true;
    }

    /**
     * Creates a SpeculativeConfigurationError with the provided message and cause.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public SpeculativeConfigurationError(String message, Throwable cause) {
        super(message, cause);
        this.fillStackTrace = true;
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
