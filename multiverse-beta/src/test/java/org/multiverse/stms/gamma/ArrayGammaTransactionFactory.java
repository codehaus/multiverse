package org.multiverse.stms.gamma;

import org.multiverse.stms.gamma.transactions.*;

import static org.multiverse.stms.gamma.ThreadLocalGammaTransactionPool.getThreadLocalGammaTransactionPool;

public class ArrayGammaTransactionFactory implements GammaTransactionFactory {

    private final GammaTransactionConfiguration config;

    public ArrayGammaTransactionFactory(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public ArrayGammaTransactionFactory(GammaTransactionConfiguration config) {
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
    public ArrayGammaTransaction newTransaction() {
        return newTransaction(getThreadLocalGammaTransactionPool());
    }

    @Override
    public ArrayGammaTransaction newTransaction(GammaTransactionPool pool) {
        ArrayGammaTransaction tx = pool.takeArrayGammaTransaction();
        if (tx == null) {
            tx = new ArrayGammaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}