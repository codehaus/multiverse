package org.multiverse.api.backoff;

import org.multiverse.api.Transaction;

/**
 * A policy that can be used to backoff when it can't make progress because other transactions are interfering.
 * A lot of interference means that there is a lot of contention and with a backoff policy it means that you can
 * reduce contention by delaying certain transactions, so that the chance increases for some of them to make progress
 * and eventually make the way free for the others.
 * <p/>
 * The {@link #delayedUninterruptible(org.multiverse.api.Transaction)} needs the transaction, so that different
 * dalays can be done based on the transaction. For example a transaction that has a higher priority could get a smaller
 * delay than a transaction with a lower priority.
 * <p/>
 * Interruptable and non interruptable delay methods are provided.
 *
 * @author Peter Veentjer.
 */
public interface BackoffPolicy {

    /**
     * Executes the delay. This call is interruptible.
     *
     * @param tx the transaction that is going to be restarted. The transaction should never be null, but it
     *           depends on the implementation if this is checked.
     * @throws NullPointerException     if t is null. It depends on the implementation if this is checked.
     * @throws IllegalArgumentException if attempt smaller than 1. It depends on the implementation if this is checked.
     * @throws InterruptedException     if the delay is interrupted.
     */
    void delay(Transaction tx) throws InterruptedException;

    /**
     * Executes the delay without the possibility of being interrupted.
     *
     * @param tx the transaction that is going to be restarted. The transaction should never be null, but it
     *           depends on the implementation if this is checked.
     * @throws NullPointerException     if t is null. It depends on the implementation if this is checked.
     * @throws IllegalArgumentException if attempt smaller than 1. It depends on the implementation if this is checked.
     */
    void delayedUninterruptible(Transaction tx);
}
