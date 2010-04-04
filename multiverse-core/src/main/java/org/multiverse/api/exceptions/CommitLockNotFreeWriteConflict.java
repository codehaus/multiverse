package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link WriteConflict} that indicates that the locks could not be acquired while doing a
 * {@link org.multiverse.api.Transaction#commit}.
 *
 * @author Peter Veentjer
 */
public class CommitLockNotFreeWriteConflict extends WriteConflict {

    private static final long serialVersionUID = 0;

    public final static CommitLockNotFreeWriteConflict INSTANCE = new CommitLockNotFreeWriteConflict();

    public final static boolean reuse = parseBoolean(getProperty(
            CommitLockNotFreeWriteConflict.class.getName() + ".reuse", "true"));

    public static CommitLockNotFreeWriteConflict createFailedToObtainCommitLocksException() {
        if (LockNotFreeReadConflict.reuse) {
            throw CommitLockNotFreeWriteConflict.INSTANCE;
        } else {
            throw new CommitLockNotFreeWriteConflict();
        }
    }

    public CommitLockNotFreeWriteConflict() {
    }

    public CommitLockNotFreeWriteConflict(String message) {
        super(message);
    }

    public CommitLockNotFreeWriteConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public CommitLockNotFreeWriteConflict(Throwable cause) {
        super(cause);
    }
}
