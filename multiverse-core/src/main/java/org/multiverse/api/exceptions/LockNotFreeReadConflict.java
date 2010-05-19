package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link ReadConflict} that indicates that a read failed because the item was locked.
 * <p/>
 * The idea is that this exception should not be thrown because you don't want a reader
 * to be blocked by a writer. But depending on the implementation it could happen that it can't
 * determine if the correct version is available. In the AlphaStm this can happen while doing the
 * actual commit, but will be removed in the near future.
 *
 * @author Peter Veentjer.
 */
public class LockNotFreeReadConflict extends ReadConflict {

    private static final long serialVersionUID = 0;

    public final static LockNotFreeReadConflict INSTANCE = new LockNotFreeReadConflict();

    public final static boolean reuse = parseBoolean(
            getProperty(LockNotFreeReadConflict.class.getName() + ".reuse", "true"));

    public static LockNotFreeReadConflict newLockNotFreeReadConflict() {
        if (LockNotFreeReadConflict.reuse) {
            throw LockNotFreeReadConflict.INSTANCE;
        } else {
            throw new LockNotFreeReadConflict();
        }
    }

    public LockNotFreeReadConflict() {
    }

    public LockNotFreeReadConflict(String message) {
        super(message);
    }

    public LockNotFreeReadConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public LockNotFreeReadConflict(Throwable cause) {
        super(cause);
    }

    @Override
    public String getDescription() {
        return "readconflict caused by a lock that was not free";
    }
}
