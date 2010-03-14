package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link ReadConflict} that indicates that a load was done, but the version needed could not be found
 * because it is too old (and doesn't exist anymore).
 * <p/>
 * In the future version history will be added so that multiple version could be alive at any
 * given moment.
 *
 * @author Peter Veentjer.
 */
public class OldVersionNotFoundReadConflict extends ReadConflict {

    private static final long serialVersionUID = 0;

    public final static OldVersionNotFoundReadConflict INSTANCE = new OldVersionNotFoundReadConflict();

    public final static boolean reuse = parseBoolean(
            getProperty(OldVersionNotFoundReadConflict.class.getName() + ".reuse", "false"));

    public OldVersionNotFoundReadConflict() {
    }

    public OldVersionNotFoundReadConflict(String message) {
        super(message);
    }

    public OldVersionNotFoundReadConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public OldVersionNotFoundReadConflict(Throwable cause) {
        super(cause);
    }
}
