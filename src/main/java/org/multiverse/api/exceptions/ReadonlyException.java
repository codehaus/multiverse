package org.multiverse.api.exceptions;

public class ReadonlyException extends IllegalTransactionStateException {

    public ReadonlyException() {
    }

    public ReadonlyException(Throwable cause) {
        super(cause);
    }

    public ReadonlyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadonlyException(String s) {
        super(s);
    }
}
