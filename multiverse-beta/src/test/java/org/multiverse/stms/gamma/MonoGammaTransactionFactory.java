package org.multiverse.stms.gamma;

import org.multiverse.stms.gamma.transactions.*;

import static org.multiverse.stms.gamma.ThreadLocalGammaTransactionPool.getThreadLocalGammaTransactionPool;

public class MonoGammaTransactionFactory implements GammaTransactionFactory {

    private final GammaTransactionConfiguration config;

    public MonoGammaTransactionFactory(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public MonoGammaTransactionFactory(GammaTransactionConfiguration config) {
        this.config = config;
    }

    @Override
    public GammaTransactionConfiguration getTransactionConfiguration() {
        return config;
    }

    @Override
    public GammaTransaction upgradeAfterSpeculativeFailure(GammaTransaction failingTransaction, GammaTransactionPool pool) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GammaTransaction newTransaction() {
        return newTransaction(getThreadLocalGammaTransactionPool());
    }

    @Override
    public MonoGammaTransaction newTransaction(GammaTransactionPool pool) {
        MonoGammaTransaction tx = pool.takeMonoGammaTransaction();
        if (tx == null) {
            tx = new MonoGammaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}
