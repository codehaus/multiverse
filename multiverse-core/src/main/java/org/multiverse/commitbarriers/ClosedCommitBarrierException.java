package org.multiverse.commitbarriers;

public class ClosedCommitBarrierException extends IllegalStateException {

    public ClosedCommitBarrierException() {
    }

    public ClosedCommitBarrierException(String s) {
        super(s);
    }

    public ClosedCommitBarrierException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClosedCommitBarrierException(Throwable cause) {
        super(cause);
    }
}
