package org.multiverse.api.lifecycle;

import org.multiverse.api.Transaction;

/**
 * @author Peter Veentjer
 */
public interface TransactionListener {

    void notify(Transaction transaction, TransactionLifecycleEvent e);
}
