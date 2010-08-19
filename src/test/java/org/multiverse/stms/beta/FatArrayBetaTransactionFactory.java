package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatArrayBetaTransaction;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public final class FatArrayBetaTransactionFactory implements BetaTransactionFactory {

    private final BetaTransactionConfiguration config;

    public FatArrayBetaTransactionFactory(BetaStm stm) {
        this(new BetaTransactionConfiguration(stm));
    }

    public FatArrayBetaTransactionFactory(BetaTransactionConfiguration config) {
        this.config = config;
    }

    @Override
    public BetaTransactionConfiguration getTransactionConfiguration() {
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
        FatArrayBetaTransaction tx = pool.takeFatArrayBetaTransaction();
        if (tx == null) {
            tx = new FatArrayBetaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}
