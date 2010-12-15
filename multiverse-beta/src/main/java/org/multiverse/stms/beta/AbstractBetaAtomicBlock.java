package org.multiverse.stms.beta;

import org.multiverse.api.AtomicBlock;
import org.multiverse.api.BackoffPolicy;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

public abstract class AbstractBetaAtomicBlock implements AtomicBlock {

    protected final BetaTransactionFactory transactionFactory;
    protected final BetaTransactionConfiguration transactionConfiguration;
    protected final BackoffPolicy backoffPolicy;

    public AbstractBetaAtomicBlock(final BetaTransactionFactory transactionFactory) {
        if (transactionFactory == null) {
            throw new NullPointerException();
        }
        this.transactionFactory = transactionFactory;
        this.transactionConfiguration = transactionFactory.getTransactionConfiguration();
        this.backoffPolicy = transactionConfiguration.backoffPolicy;
    }
}
