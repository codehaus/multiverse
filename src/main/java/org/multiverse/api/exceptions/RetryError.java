package org.multiverse.api.exceptions;

public class RetryError extends ControlFlowError {
    public final static RetryError INSTANCE = new RetryError();
}
