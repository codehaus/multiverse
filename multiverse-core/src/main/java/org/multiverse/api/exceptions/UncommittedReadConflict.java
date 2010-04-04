package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * An {@link ReadConflict} that is thrown when an object is loaded but has not been committed yet.
 * There are 2 why reasons this can happen:
 * <ol>
 * <li>
 * an TransactionalObject has escaped and is used by another transaction, before the transaction creating
 * that object has committed.
 * </li>
 * <li>
 * an TransactionalObject is used, after the transaction in which the TransactionalObject is created is aborted.
 * </li>
 * </ol>
 * In either case, the cause is a misuse of the STM, so a programming error and is not recoverable.
 * <p/>
 * There is no reason for this exception to be reused is because this exception indicates a
 * programming failure, and you want to have good feedback when that happens.
 *
 * @author Peter Veentjer.
 */
public class UncommittedReadConflict extends ReadConflict {

    public final static UncommittedReadConflict INSTANCE = new UncommittedReadConflict();

    public final static boolean reuse = parseBoolean(getProperty(
            UncommittedReadConflict.class.getName() + ".reuse", "true"));

    public static UncommittedReadConflict createUncommittedReadConflict() {
        if (LockNotFreeReadConflict.reuse) {
            throw UncommittedReadConflict.INSTANCE;
        } else {
            throw new UncommittedReadConflict();
        }
    }

    private static final long serialVersionUID = 0;

    public UncommittedReadConflict() {
    }

    public UncommittedReadConflict(String message) {
        super(message);
    }

    public UncommittedReadConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public UncommittedReadConflict(Throwable cause) {
        super(cause);
    }

    public static Exception create() {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }
}
