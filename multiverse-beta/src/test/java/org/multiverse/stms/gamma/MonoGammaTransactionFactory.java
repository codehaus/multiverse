package org.multiverse.stms.gamma;

import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.GammaTransactionPool;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransaction;

import static org.multiverse.stms.gamma.ThreadLocalGammaTransactionPool.getThreadLocalGammaTransactionPool;

public class MonoGammaTransactionFactory implements GammaTransactionFactory {

    private final GammaTransactionConfiguration config;

    public MonoGammaTransactionFactory(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm).setControlFlowErrorsReused(false));
    }

    public MonoGammaTransactionFactory(GammaTransactionConfiguration config) {
        this.config = config.setControlFlowErrorsReused(false);
    }

    @Override
    public GammaTransactionConfiguration getConfiguration() {
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
    public FatMonoGammaTransaction newTransaction(GammaTransactionPool pool) {
        FatMonoGammaTransaction tx = pool.takeMonoGammaTransaction();
        if (tx == null) {
            tx = new FatMonoGammaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}
