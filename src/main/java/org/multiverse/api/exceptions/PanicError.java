package org.multiverse.api.exceptions;


public class PanicError extends Error {

    public PanicError() {
    }

    public PanicError(String message) {
        super(message);
    }

    public PanicError(String message, Throwable cause) {
        super(message, cause);
    }

    public PanicError(Throwable cause) {
        super(cause);
    }
}
