package org.multiverse.stms.gamma;

import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.GammaTransactionPool;
import org.multiverse.stms.gamma.transactions.fat.FatFixedLengthGammaTransaction;

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
    public FatFixedLengthGammaTransaction newTransaction() {
        return newTransaction(getThreadLocalGammaTransactionPool());
    }

    @Override
    public FatFixedLengthGammaTransaction newTransaction(GammaTransactionPool pool) {
        FatFixedLengthGammaTransaction tx = pool.takeFatFixedLength();
        if (tx == null) {
            tx = new FatFixedLengthGammaTransaction(config);
        } else {
            tx.init(config);
        }
        return tx;
    }
}