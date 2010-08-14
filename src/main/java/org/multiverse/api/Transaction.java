package org.multiverse.api;

import org.multiverse.api.blocking.Latch;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;

/**
 * @author Peter Veentjer
 */
public interface Transaction {

    TransactionConfiguration getConfiguration();

    TransactionStatus getStatus();

    int getAttempt();

    void start();

    void commit();

    void abort();

    void prepare();

    /**
     * Resets the transaction so that it can be reused for another attempt to evaluate the transaction.
     *
     * @return true if another attempt can be made, false otherwise.
     */
    boolean softReset();

    /**
     * Resets the transaction so that it can be reused.
     */
    void hardReset();

    void startEitherBranch();

    void endEitherBranch();

    void startOrElseBranch();

    void registerChangeListenerAndAbort(Latch changeListener);

    void init(BetaTransactionConfig transactionConfig);

    void register(TransactionLifecycleListener listener);

    /**
     * Registers a permanent TransactionLifecycleListener. The only difference between this method and the
     * {@link #register(TransactionLifecycleListener)} is that this listener is not removed if the transaction
     * is reset for another attempt. But it is removed
     * <p/>
     * If a TransactionLifecycleListener is added more than once, it is executed more than once. No checks
     * are made. The permanent listeners are executed in the order they are added.
     *
     * @param listener the listener to add.
     * @throws NullPointerException if listener is null
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the correct
     *                              state (e.g. aborted or committed).
     */
    void registerPermanent(TransactionLifecycleListener listener);

    long getRemainingTimeoutNs();

    void setRemainingTimeoutNs(long timeoutNs);
}
