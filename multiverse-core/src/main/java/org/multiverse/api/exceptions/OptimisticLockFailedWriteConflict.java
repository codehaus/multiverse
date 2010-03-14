package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link WriteConflict} that indicates that the version of the transactional object
 * you want to update already was updated by another transaction.
 *
 * @author Peter Veentjer.
 */
public class OptimisticLockFailedWriteConflict extends WriteConflict {

    private static final long serialVersionUID = 0;

    public final static OptimisticLockFailedWriteConflict INSTANCE = new OptimisticLockFailedWriteConflict();

    public final static boolean reuse = parseBoolean(
            getProperty(OptimisticLockFailedWriteConflict.class.getName() + ".reuse", "true"));

    public OptimisticLockFailedWriteConflict() {
    }

    public OptimisticLockFailedWriteConflict(String message) {
        super(message);
    }

    public OptimisticLockFailedWriteConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public OptimisticLockFailedWriteConflict(Throwable cause) {
        super(cause);
    }
}
