package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link WriteConflict} that is thrown when a writeskew is detected and the transaction is
 * not allowed to commit and therefore aborted.
 *
 * @author Peter Veentjer
 */
public class WriteSkewConflict extends WriteConflict {

    private static final long serialVersionUID = 0;

    public final static boolean reuse = parseBoolean(getProperty(WriteSkewConflict.class.getName() + ".reuse", "true"));

    public final static WriteSkewConflict INSTANCE = new WriteSkewConflict();

    public WriteSkewConflict() {
    }

    public WriteSkewConflict(Throwable cause) {
        super(cause);
    }

    public WriteSkewConflict(String message) {
        super(message);
    }

    public WriteSkewConflict(String message, Throwable cause) {
        super(message, cause);
    }
}
