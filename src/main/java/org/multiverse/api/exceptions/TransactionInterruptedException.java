package org.multiverse.api.exceptions;

/**
 * A {@link RetryException} thrown when the blocking operation on a transaction using the retry has
 * been interrupted.
 *
 * Unlike the {@link InterruptedException} this exception is not checked. A checked interrupted
 * exception is quite nasty to have since either you need to deal with it, or you need to propagate it.
 *
 * In most cases you are not able to deal with it, but want it to be propagated. A checked exception
 * is nasty to propagate. 
 *
 * @author Peter Veentjer.
 */
public class TransactionInterruptedException extends RetryException{
    public TransactionInterruptedException() {
    }

    public TransactionInterruptedException(String message) {
        super(message);
    }

    public TransactionInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionInterruptedException(Throwable cause) {
        super(cause);
    }
}
