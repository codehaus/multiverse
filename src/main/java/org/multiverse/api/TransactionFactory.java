package org.multiverse.api;

import org.multiverse.stms.beta.transactions.BetaTransaction;

/**
 * @author Peter Veentjer
 */
public interface TransactionFactory {

    BetaTransaction start();
}
