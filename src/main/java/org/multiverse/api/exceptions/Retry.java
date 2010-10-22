package org.multiverse.api.exceptions;

/**
 * A {@link ControlFlowError} that indicates that an explicit retry should be done, e.g. because
 * a transaction wants to pop an item from an empty queue. The RetryError is caught by the transaction
 * handling logic (e.g the {@link org.multiverse.api.AtomicBlock}.
 * <p/>
 * An Retry instance is available, since the Retry is thrown when the {@link org.multiverse.api.StmUtils#retry()}
 * is called and this is used for control flow. Creating exceptions is quite expensive (especially the stacktrace)
 * so that is why a pre-existing instance is created.
 *
 * @author Peter Veentjer.
 */
public class Retry extends ControlFlowError {

    private static final long serialVersionUID = 0;

    public final static Retry INSTANCE = new Retry();

    public Retry() {
    }
}
