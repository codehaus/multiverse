package org.multiverse.api.closures;

import org.multiverse.api.Transaction;

/**
 * An AtomicClosure tailored for void
 *
 * @author Peter Veentjer.
 */
public interface AtomicVoidClosure{

    /**
     * Executes the closure.
     *
     * @param tx the Transaction. Depending on the TransactionPropagation level, this could
     *           be null.
     * @return the result of the closure.
     * @throws Exception
     */
    void execute(Transaction tx)throws Exception;
}
