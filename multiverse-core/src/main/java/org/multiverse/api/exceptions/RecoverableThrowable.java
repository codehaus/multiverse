package org.multiverse.api.exceptions;

/**
 * A marker interface that can be added to exceptions to indicate that the transaction is likely to
 * have better luck next time. An example of such an exception is the 
 * {@link WriteConflictException} that is thrown to indicate that the Transaction didn't 
 * succeed this time, but next time there is a chance it will.
 * <p/>
 * Since Java doesn't support multiple inheritance, the RecoverableThrowable is an interface.
 *
 * @author Peter Veentjer.
 */
public interface RecoverableThrowable {
}
