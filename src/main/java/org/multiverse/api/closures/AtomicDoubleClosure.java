package org.multiverse.api.closures;

import org.multiverse.api.Transaction;

/**
* An AtomicClosure tailored for double
*
* @author Peter Veentjer.
*/
public interface AtomicDoubleClosure{

/**
* Executes the closure.
*
* @param tx the Transaction. Depending on the TransactionPropagation level, this could
*           be null.
* @return the result of the closure.
* @throws Exception
*/
double execute(Transaction tx)throws Exception;
}
