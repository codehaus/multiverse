package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link WriteConflict} that indicates that the version of the transactional object
 * you want to update already was updated by another transaction.
 *
 * @author Peter Veentjer.
 */
public class VersionTooOldWriteConflict extends WriteConflict {

    private static final long serialVersionUID = 0;

    public final static VersionTooOldWriteConflict INSTANCE = new VersionTooOldWriteConflict();

    public final static boolean reuse = parseBoolean(
            getProperty(VersionTooOldWriteConflict.class.getName() + ".reuse", "true"));

    public VersionTooOldWriteConflict() {
    }

    public VersionTooOldWriteConflict(String message) {
        super(message);
    }

    public VersionTooOldWriteConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionTooOldWriteConflict(Throwable cause) {
        super(cause);
    }
}
