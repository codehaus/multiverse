package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link ControlFlowError} that indicates that an explicit retry should be done, e.g. because
 * a transaction wants to pop an item from an empty queue.
 *
 * @author Peter Veentjer.
 */
public class RetryError extends ControlFlowError {

    private static final long serialVersionUID = 0;

    private final static boolean reuse = parseBoolean(
            getProperty(RetryError.class.getName() + ".reuse", "true"));

    public final static RetryError INSTANCE = new RetryError();

    public static RetryError create() {
        if (reuse) {
            return RetryError.INSTANCE;
        } else {
            return new RetryError();
        }
    }
}
