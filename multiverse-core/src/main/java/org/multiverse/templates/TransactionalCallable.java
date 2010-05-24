package org.multiverse.templates;

import org.multiverse.api.Transaction;

/**
 * A callable is executed using a {@link Transaction}. It essentially is the same as the
 * {@link java.util.concurrent.Callable}. The only differences is that the transaction is passed
 * to the {@link #call(org.multiverse.api.Transaction)} method.
 * <p/>
 * The TransactionalCallable can be used in combination with the {@link TransactionBoilerplate}.
 *
 * @author Peter Veentjer
 */
public interface TransactionalCallable<V> {

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @param tx the Transaction
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    V call(Transaction tx) throws Exception;
}
