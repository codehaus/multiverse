package org.multiverse.api.exceptions;

/**
 * An {@link IllegalStateException} that is thrown when an object is loaded but has not been committed yet.
 * There are 2 why reasons this can happen:
 * <ol>
 * <li>
 * an AtomicObject has escaped and is used by another transaction, before the transaction creating
 * that object has committed.
 * </li>
 * <li>
 * an AtomicObject is used, after the transaction in which the atomicobject is created is aborted.
 * </li>
 * </ol>
 * In either case, the cause is a misuse of the STM, so a programming error and is not recoverable.
 * <p/>
 * There is no reason for this exception to be reused is because this exception indicates a
 * programming failure, and you want to have good feedback when that happens.
 *
 * @author Peter Veentjer.
 */
public class LoadUncommittedException extends LoadException implements RecoverableThrowable {

    private static final long serialVersionUID = 0;

    public LoadUncommittedException() {
    }

    public LoadUncommittedException(String message) {
        super(message);
    }

    public LoadUncommittedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadUncommittedException(Throwable cause) {
        super(cause);
    }
}
