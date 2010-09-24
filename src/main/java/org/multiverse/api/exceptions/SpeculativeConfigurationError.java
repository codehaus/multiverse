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

    public final static SpeculativeConfigurationError INSTANCE = new SpeculativeConfigurationError();

    /**
     * Creates a SpeculativeConfigurationError.
     */
    public SpeculativeConfigurationError() {
    }

    /**
     * Creates a SpeculativeConfigurationError with the provided message.
     *
     * @param message the message of the exception.
     */
    public SpeculativeConfigurationError(String message) {
        super(message);
    }

    /**
     * Creates a SpeculativeConfigurationError with the provided message and cause.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public SpeculativeConfigurationError(String message, Throwable cause) {
        super(message, cause);
    }
}
