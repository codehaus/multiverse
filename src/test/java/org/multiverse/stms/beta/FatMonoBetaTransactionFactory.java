package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.multiverse.stms.beta.ThreadLocalBetaTransactionPool.getThreadLocalBetaTransactionPool;

public final class FatMonoBetaTransactionFactory implements BetaTransactionFactory {
    private final BetaTransactionConfiguration config;

    public FatMonoBetaTransactionFactory(BetaStm stm) {
        this(new BetaTransactionConfiguration(stm));
    }

    public FatMonoBetaTransactionFactory(BetaTransactionConfiguration config) {
        this.config = config;
    }

    @Override
    public BetaTransactionConfiguration getTransactionConfiguration() {
        return config;
    }

    @Override
    public BetaTransaction upgradeAfterSpeculativeFailure(BetaTransaction failingTransaction, BetaTransactionPool pool) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BetaTransaction start() {
        return start(getThreadLocalBetaTransactionPool());
    }

    @Override
    public BetaTransaction start(BetaTransactionPool pool) {
        FatMonoBetaTransaction tx = pool.takeFatMonoBetaTransaction();
        if (tx == null) {
            tx = new FatMonoBetaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}
