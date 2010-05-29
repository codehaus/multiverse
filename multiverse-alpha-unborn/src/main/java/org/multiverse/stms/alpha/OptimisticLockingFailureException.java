package org.multiverse.stms.alpha;

/**
 * A {@link RuntimeException} that indicates that an optimistic locking failure happened.
 * <p/>
 * A optimistic locking failure is thrown when the version of a tranlocal is read in one transaction,
 * and used together with {@link org.multiverse.stms.alpha.programmatic.AlphaProgrammaticRef#atomicCompareAndSet(Object, long)}.
 *
 * @author Peter Veentjer
 */
public class OptimisticLockingFailureException extends RuntimeException {

    public OptimisticLockingFailureException() {
    }

    public OptimisticLockingFailureException(Throwable cause) {
        super(cause);
    }

    public OptimisticLockingFailureException(String message) {
        super(message);
    }

    public OptimisticLockingFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
