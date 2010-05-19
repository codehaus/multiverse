package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link ControlFlowError} that indicates that an explicit retry should be done, e.g. because
 * a transaction wants to pop an item from an empty queue.
 *
 * @author Peter Veentjer.
 */
public class Retry extends ControlFlowError {

    private static final long serialVersionUID = 0;

    private final static boolean reuse = parseBoolean(getProperty(Retry.class.getName() + ".reuse", "true"));

    public final static Retry INSTANCE = new Retry();

    public static Retry create() {
        if (reuse) {
            return Retry.INSTANCE;
        } else {
            return new Retry();
        }
    }

    @Override
    public String getDescription() {
        return "retry";
    }
}
