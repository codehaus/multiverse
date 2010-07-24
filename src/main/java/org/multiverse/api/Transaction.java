package org.multiverse.api;

import org.multiverse.api.blocking.Latch;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;

/**
 * @author Peter Veentjer
 */
public interface Transaction {

    int getAttempt();

    void incAttempt();

    void resetAttempt();

    void start();

    void commit();

    void abort();

    void prepare();

    void reset();

    void startEitherBranch();

    void endEitherBranch();

    void startOrElseBranch();

    void registerChangeListenerAndAbort(Latch changeListener);

    void init(BetaTransactionConfig transactionConfig);

    TransactionStatus getStatus();

    void register(TransactionLifecycleListener listener);

    void registerPermanent(TransactionLifecycleListener listener);
}
