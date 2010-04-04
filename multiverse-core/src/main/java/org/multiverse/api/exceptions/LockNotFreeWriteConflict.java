package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link WriteConflict} that indicates that the locks could not be acquired while doing a
 * {@link org.multiverse.api.Transaction#commit}.
 *
 * @author Peter Veentjer
 */
public class LockNotFreeWriteConflict extends WriteConflict {

    private static final long serialVersionUID = 0;

    public final static LockNotFreeWriteConflict INSTANCE = new LockNotFreeWriteConflict();

    public final static boolean reuse = parseBoolean(getProperty(
            LockNotFreeWriteConflict.class.getName() + ".reuse", "true"));

    public static LockNotFreeWriteConflict createFailedToObtainCommitLocksException() {
        if (LockNotFreeReadConflict.reuse) {
            throw LockNotFreeWriteConflict.INSTANCE;
        } else {
            throw new LockNotFreeWriteConflict();
        }
    }

    public LockNotFreeWriteConflict() {
    }

    public LockNotFreeWriteConflict(String message) {
        super(message);
    }

    public LockNotFreeWriteConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public LockNotFreeWriteConflict(Throwable cause) {
        super(cause);
    }
}
