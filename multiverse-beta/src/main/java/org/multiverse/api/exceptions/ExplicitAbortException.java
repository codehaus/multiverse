package org.multiverse.api.exceptions;

public class ExplicitAbortException extends IllegalTransactionStateException {

    public ExplicitAbortException() {
    }

    public ExplicitAbortException(String message) {
        super(message);
    }

    public ExplicitAbortException(String message, Throwable cause) {
        super(message, cause);
    }
}
