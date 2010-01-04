package org.multiverse.utils.restartbackoff;

import org.multiverse.api.Transaction;

/**
 * A policy that can be used to backoff when a transaction needs to be restarted. A transaction restart normally is an
 * indication that there is contention, and by delaying the restart, other transactions are able to complete.
 * <p/>
 * The {@link #backoffUninterruptible(org.multiverse.api.Transaction , int)} needs the transaction, so that different
 * dalays can be done based on the transaction. For example a transaction that has a higher priority could get a smaller
 * delay than a transaction with a lower priority.
 * <p/>
 * Interruptable and non interruptable delay methods are provided.
 *
 * @author Peter Veentjer.
 */
public interface RestartBackoffPolicy {

    /**
     * Executes the delay. This call is interruptible.
     *
     * @param t       the transaction that is going to be restarted. The transaction should never be null, but it
     *                depends on the implementation if this is checked.
     * @param attempt indicates the number of times the transaction is tried. Attempt should always be equal or larger
     *                than 1. It depends on the implementation if this is checked.
     * @throws NullPointerException     if t is null. It depends on the implementation if this is checked.
     * @throws IllegalArgumentException if attempt smaller than 1. It depends on the implementation if this is checked.
     * @throws InterruptedException     if the delay is interrupted.
     */
    void delay(Transaction t, int attempt) throws InterruptedException;

    /**
     * Executes the delay without the possibility of being interrupted.
     *
     * @param t       the transaction that is going to be restarted. The transaction should never be null, but it
     *                depends on the implementation if this is checked.
     * @param attempt indicates the number of times the transaction is tried. Attempt should always be equal or larger
     *                than 1. It depends on the implementation if this is checked.
     * @throws NullPointerException     if t is null. It depends on the implementation if this is checked.
     * @throws IllegalArgumentException if attempt smaller than 1. It depends on the implementation if this is checked.
     */
    void backoffUninterruptible(Transaction t, int attempt);
}
