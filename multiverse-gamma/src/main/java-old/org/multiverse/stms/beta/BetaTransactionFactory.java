package org.multiverse.stms.beta;

import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.BetaTransactionPool;

/**
 * A {@link TransactionFactory} specific for the BetaStm.
 *
 * @author Peter Veentjer.
 */
public interface BetaTransactionFactory extends TransactionFactory {

    @Override
    BetaTransactionConfiguration getTransactionConfiguration();

    @Override
    BetaTransaction newTransaction();

    /**
     * Creates a new BetaTransaction.
     *
     * @param pool the BetaObjectPool used to get a pooled transaction from if one is available.
     * @return the started BetaTransaction.
     */
    BetaTransaction newTransaction(BetaTransactionPool pool);

    /**
     * Upgrades the transaction after a speculative failure happened.
     *
     * @param failingTransaction the transaction that failed.
     * @param pool               the BetaObjectPool used to pool stuff
     * @return the upgraded BetaTransaction.
     * @throws UnsupportedOperationException if this BetaTransactionFactory doesn't support speculative
     *                                       behavior.
     */
    BetaTransaction upgradeAfterSpeculativeFailure(BetaTransaction failingTransaction, BetaTransactionPool pool);
}
