package org.multiverse.stms.gamma;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;

public abstract class AbstractGammaAtomicBlock implements GammaAtomicBlock{
    protected final GammaTransactionFactory transactionFactory;
    protected final GammaTransactionConfiguration transactionConfiguration;
    protected final BackoffPolicy backoffPolicy;

    public AbstractGammaAtomicBlock(final GammaTransactionFactory transactionFactory) {
        if (transactionFactory == null) {
            throw new NullPointerException();
        }
        this.transactionFactory = transactionFactory;
        this.transactionConfiguration = transactionFactory.getTransactionConfiguration();
        this.backoffPolicy = transactionConfiguration.backoffPolicy;
    }
}
