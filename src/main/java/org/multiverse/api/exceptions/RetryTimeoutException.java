package org.multiverse.api.exceptions;

public class RetryTimeoutException extends RuntimeException {

    public RetryTimeoutException(String msg) {
        super(msg);
    }

    public RetryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

