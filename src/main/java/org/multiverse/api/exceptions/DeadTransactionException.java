package org.multiverse.api.exceptions;

/**
 * @author Peter Veentjer
 */

public class DeadTransactionException extends IllegalTransactionStateException{
    
    public DeadTransactionException(String s) {
        super(s);
    }

    public DeadTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeadTransactionException() {
    }
}
