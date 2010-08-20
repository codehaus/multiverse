package org.multiverse.api.exceptions;

public class NoTransactionAllowedException extends PropagationException{
    public NoTransactionAllowedException() {
    }

    public NoTransactionAllowedException(String s) {
        super(s);
    }

    public NoTransactionAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoTransactionAllowedException(Throwable cause) {
        super(cause);
    }
}
