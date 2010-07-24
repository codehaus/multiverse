package org.multiverse.api.lifecycle;

import org.multiverse.api.Transaction;

/**
 * @author Peter Veentjer
 */
public interface TransactionLifecycleListener {

    void notify(Transaction transaction, TransactionLifecycleEvent e);
}
