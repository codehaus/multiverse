package org.multiverse.stms.beta;

import org.multiverse.api.Transaction;

/**
 * @author Peter Veentjer
 */
public interface BetaTransaction extends Transaction {

    BetaTranlocal load(BetaAtomicObject atomicObject);

}


