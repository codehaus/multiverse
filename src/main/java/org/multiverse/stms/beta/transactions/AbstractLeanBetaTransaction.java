package org.multiverse.stms.beta.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import java.util.ArrayList;

public abstract class AbstractLeanBetaTransaction extends BetaTransaction {

    public AbstractLeanBetaTransaction(int poolTransactionType, BetaTransactionConfiguration config) {
        super(poolTransactionType, config);
    }

    @Override
    public final ArrayList<TransactionLifecycleListener> getNormalListeners() {
        return null;
    }

    @Override
    public final void copyForSpeculativeFailure(BetaTransaction tx) {
        remainingTimeoutNs = tx.getRemainingTimeoutNs();
        attempt = tx.getAttempt();
    }

    @Override
    public final void register(final TransactionLifecycleListener listener) {
        if (listener == null) {
            abort();
            throw new NullPointerException();
        }

        switch (status) {
            case NEW:
            case ACTIVE:
            case PREPARED:
                config.needsListeners();
                abort();
                throw SpeculativeConfigurationError.INSTANCE;
            case COMMITTED:
                throw new DeadTransactionException();
            case ABORTED:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }
}
