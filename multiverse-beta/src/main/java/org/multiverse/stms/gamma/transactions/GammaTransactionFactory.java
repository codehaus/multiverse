package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.TransactionFactory;

public interface GammaTransactionFactory extends TransactionFactory {

    @Override
    GammaTransactionConfiguration getTransactionConfiguration();

    GammaTransaction newTransaction(GammaTransactionPool pool);

    GammaTransaction upgradeAfterSpeculativeFailure(GammaTransaction tx, GammaTransactionPool pool);
}
