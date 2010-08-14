package org.multiverse.api;

import org.multiverse.api.exceptions.NoTransactionFoundException;

/**
 * A {@link ThreadLocal} that contains the current {@link Transaction}. The {@link Stm} and the {@link Transaction}
 * should not rely on threadlocals, they are only used for convenience to reduce the need to carry around a
 * Transaction.
 *
 * @author Peter Veentjer.
 */
public final class ThreadLocalTransaction {

    public final static ThreadLocal<Transaction> threadlocal = new ThreadLocal<Transaction>();

    /**
     * Gets the threadlocal transaction. If no transaction is set, null is returned.
     * <p/>
     * No checks are done on the state of the transaction (so it could be that an aborted or committed transaction is
     * returned).
     *
     * @return the threadlocal transaction.
     */
    public static Transaction getThreadLocalTransaction() {
        return threadlocal.get();
    }

    /**
     * Gets the threadlocal transaction or throws a NoTransactionException if no transaction is found.
     * <p/>
     * No checks are done on the state of the transaction (so it could be that an aborted or committed transaction is
     * returned).
     *
     * @return the threadlocal transaction.
     * @throws NoTransactionFoundException if no thread local transaction is found.
     */
    public static Transaction getRequiredThreadLocalTransaction() {
        Transaction tx = threadlocal.get();

        if (tx == null) {
            throw new NoTransactionFoundException("No transaction is found on the ThreadLocalTransaction");
        }

        return tx;
    }

    /**
     * Clears the threadlocal transaction.
     * <p/>
     * If a transaction is available, it isn't aborted or committed.
     */
    public static void clearThreadLocalTransaction() {
        threadlocal.set(null);
    }

    /**
     * Sets the threadlocal transaction. The transaction is allowed to be null, effectively clearing the
     * current thread local transaction.
     * <p/>
     * If a transaction is available, it isn't aborted or committed.
     *
     * @param tx the new thread local transaction.
     */
    public static void setThreadLocalTransaction(Transaction tx) {
        threadlocal.set(tx);
    }

    //we don't want any instances.

    private ThreadLocalTransaction() {
    }
}
