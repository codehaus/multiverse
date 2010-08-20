package org.multiverse.api.exceptions;

public class PropagationException extends IllegalStateException{
    public PropagationException() {
    }

    public PropagationException(String s) {
        super(s);
    }

    public PropagationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropagationException(Throwable cause) {
        super(cause);
    }
}
