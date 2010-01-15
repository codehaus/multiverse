package org.multiverse.api;

/**
 * With the {@link org.multiverse.api.TransactionLifecycleListener} it is possible to listen
 * to certain events in the transaction (aborting/committing).
 *
 * @author Peter Veentjer
 */
public interface TransactionLifecycleListener {

    /**
     * Is called by the Transaction when the event occurs. If an exception is thrown while executing
     * this method, it could be that other listeners are not notified.
     * <p/>
     * If this method fails before committing, the transaction is aborted no matter what.
     *
     * @param tx    the Transaction that triggered the event.
     * @param event the TransactionLifecycleEvent t is currently in.
     */
    void notify(Transaction tx, TransactionLifecycleEvent event);
}
