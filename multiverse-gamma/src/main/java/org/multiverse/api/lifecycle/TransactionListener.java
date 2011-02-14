package org.multiverse.api.lifecycle;

import org.multiverse.api.Transaction;

/**
 * A listener tailored for listening to events in the transaction lifecycle.
 *
 * @author Peter Veentjer
 * @see TransactionEvent
 * @see org.multiverse.api.TransactionConfiguration#getPermanentListeners()
 * @see Transaction#register(TransactionListener)
 */
public interface TransactionListener {

    /**
     * Notifies that a certain event happened inside a transaction.
     *
     * @param transaction the transaction where the event happened
     * @param e the event
     */
    void notify(Transaction transaction, TransactionEvent e);
}
