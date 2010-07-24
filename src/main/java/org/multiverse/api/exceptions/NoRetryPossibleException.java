package org.multiverse.api.exceptions;

public class NoRetryPossibleException extends IllegalTransactionStateException{

    public NoRetryPossibleException() {
    }

    public NoRetryPossibleException(String message) {
        super(message);
    }

    public NoRetryPossibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoRetryPossibleException(Throwable cause) {
        super(cause);
    }
}
