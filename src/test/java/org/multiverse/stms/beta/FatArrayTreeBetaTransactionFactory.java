package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactions.FatArrayTreeBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public final class FatArrayTreeBetaTransactionFactory implements BetaTransactionFactory {
    private final BetaTransactionConfig config;

    public FatArrayTreeBetaTransactionFactory(BetaStm stm) {
        this(new BetaTransactionConfig(stm));
    }

    public FatArrayTreeBetaTransactionFactory(BetaTransactionConfig config) {
        this.config = config;
    }

    @Override
    public BetaTransactionConfig getTransactionConfiguration() {
        return config;
    }

    @Override
    public BetaTransaction start() {
        return start(getThreadLocalBetaObjectPool());
    }

    @Override
    public BetaTransaction start(BetaObjectPool pool) {
        FatArrayTreeBetaTransaction tx = pool.takeFatArrayTreeBetaTransaction();
        if (tx == null) {
            tx = new FatArrayTreeBetaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }

    @Override
    public BetaTransaction upgradeAfterSpeculativeFailure(BetaTransaction failingTransaction, BetaObjectPool pool) {
        throw new UnsupportedOperationException();
    }
}
