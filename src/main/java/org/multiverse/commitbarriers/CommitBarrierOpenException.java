package org.multiverse.commitbarriers;

/**
 * An IllegalStateException that indicates that an operation was executed on the CommitBarrier while it
 * already is opened.
 *
 * @author Peter Veentjer.
 */
public class CommitBarrierOpenException extends IllegalStateException {

    public CommitBarrierOpenException() {
    }

    public CommitBarrierOpenException(String s) {
        super(s);
    }

    public CommitBarrierOpenException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommitBarrierOpenException(Throwable cause) {
        super(cause);
    }
}
