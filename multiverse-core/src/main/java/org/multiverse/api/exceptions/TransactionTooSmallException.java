package org.multiverse.api.exceptions;

public class TransactionTooSmallException extends RuntimeException {

    public static final TransactionTooSmallException INSTANCE = new TransactionTooSmallException();

    public TransactionTooSmallException() {
    }

    public TransactionTooSmallException(String message) {
        super(message);
    }

    public TransactionTooSmallException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionTooSmallException(Throwable cause) {
        super(cause);
    }
}
