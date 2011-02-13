package org.multiverse.api.exceptions;

/**
 * A {@link ControlFlowError} that indicates that an explicit retry should be done, e.g. because
 * a transaction wants to pop an item from an empty queue. The Retry is caught by the transaction
 * handling logic (e.g the {@link org.multiverse.api.AtomicBlock}.
 *
 * @author Peter Veentjer.
 */
public class RetryError extends ControlFlowError {

    private static final long serialVersionUID = 0;

    public final static RetryError INSTANCE = new RetryError(false);

    /**
     * Creates a new Retry Error.
     *
     * @param fillStackTrace if the StackTrace should be filled.
     */
    public RetryError(boolean fillStackTrace) {
        super(fillStackTrace);
    }
}
