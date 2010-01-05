package org.multiverse.api;

/**
 * With the {@link org.multiverse.api.TransactionLifecycleListener} it is possible to listen
 * to certain events in the transaction (aborting/committing).
 *
 * The TransactionLifecycleListener  is an abstract class instead of an interface so
 * that is can be used as a linked list inside the stm without the need to create
 * a list and node objects. So it is optimized by reducing object creation. A
 * TransactionLifecycleListener should not be reused because of this next field.
 *
 * @author Peter Veentjer
 */
public abstract class TransactionLifecycleListener {

    public TransactionLifecycleListener next;

    /**
     * Is called by the Transaction when the event occurs. If an exception is thrown while executing
     * this method, it could be that other listeners are not notified.
     *
     * If this method fails before committing, the transaction is aborted no matter what.      
     *
     * @param tx the Transaction that triggered the event.
     * @param event the TransactionLifecycleEvent t is currently in.
     */
    public  abstract void notify(Transaction tx, TransactionLifecycleEvent event);
}
