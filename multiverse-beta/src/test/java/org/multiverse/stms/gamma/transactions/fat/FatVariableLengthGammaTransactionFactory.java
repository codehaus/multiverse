package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.GammaTransactionPool;

import static org.multiverse.stms.gamma.ThreadLocalGammaTransactionPool.getThreadLocalGammaTransactionPool;

public final class FatVariableLengthGammaTransactionFactory implements GammaTransactionFactory {
    private final GammaTransactionConfiguration config;

    public FatVariableLengthGammaTransactionFactory(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public FatVariableLengthGammaTransactionFactory(GammaTransactionConfiguration config) {
        this.config = config;
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
    public GammaTransaction newTransaction(GammaTransactionPool pool) {
        GammaTransaction tx = pool.takeMap();
        if (tx == null) {
            tx = new FatVariableLengthGammaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}
