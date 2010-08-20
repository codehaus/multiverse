package org.multiverse.api.exceptions;

/**
 * A {@link ControlFlowError} that indicates that an explicit retry should be done, e.g. because
 * a transaction wants to pop an item from an empty queue. The RetryError is caught by the transaction
 * handling logic (e.g the {@link org.multiverse.api.AtomicBlock}.
 *
 * @author Peter Veentjer.
 */
public class Retry extends ControlFlowError {

    public final static Retry INSTANCE = new Retry();

    public Retry() {
    }
}
