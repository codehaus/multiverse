package org.multiverse.stms.gamma;

import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.GammaTransactionPool;
import org.multiverse.stms.gamma.transactions.fat.FatMapGammaTransaction;

import static org.multiverse.stms.gamma.ThreadLocalGammaTransactionPool.getThreadLocalGammaTransactionPool;

public final class MapGammaTransactionFactory implements GammaTransactionFactory {
    private final GammaTransactionConfiguration config;

    public MapGammaTransactionFactory(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public MapGammaTransactionFactory(GammaTransactionConfiguration config) {
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
        GammaTransaction tx = pool.takeMapGammaTransaction();
        if (tx == null) {
            tx = new FatMapGammaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}
