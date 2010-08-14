package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public final class FatMonoBetaTransactionFactory implements BetaTransactionFactory {
    private final BetaTransactionConfig config;

    public FatMonoBetaTransactionFactory(BetaStm stm){
        this(new BetaTransactionConfig(stm));
    }

    public FatMonoBetaTransactionFactory(BetaTransactionConfig config) {
        this.config = config;
    }

    @Override
    public BetaTransactionConfig getTransactionConfiguration() {
        return config;
    }

    @Override
    public BetaTransaction upgradeAfterSpeculativeFailure(BetaTransaction failingTransaction, BetaObjectPool pool) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BetaTransaction start() {
        return start(getThreadLocalBetaObjectPool());
    }

    @Override
    public BetaTransaction start(BetaObjectPool pool) {
        FatMonoBetaTransaction tx = pool.takeFatMonoBetaTransaction();
        if (tx == null) {
            tx = new FatMonoBetaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}
