package org.multiverse.api;

import org.multiverse.api.exceptions.Retry;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;

/**
 * A utility class with convenience methods to access the {@link org.multiverse.api.Stm} or
 * {@link Transaction}. These methods can be imported using the static import for a less
 * ugly (but potentially more confusing) syntax.
 *
 * @author Peter Veentjer.
 */
public final class StmUtils {

    /**
     * Does a retry.
     * <p/>
     * Under the hood the retry throws an Retry that will be caught up the chain
     * (by the TransactionTemplate for example).
     *
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException
     *          if no transaction is found in the ThreadLocalTransaction.
     */
    public static void retry() {
        getRequiredThreadLocalTransaction();
        throw Retry.create();
    }

    /**
     * Prepares the Transaction in the ThreadLocalTransaction transaction.
     * <p/>
     * For more information see {@link Transaction#prepare()}.
     */
    public static void prepare() {
        Transaction tx = getRequiredThreadLocalTransaction();
        tx.prepare();
    }

    /**
     * Aborts the Transaction in the ThreadLocalTransaction transaction.
     * <p/>
     * For more information see {@link Transaction#abort()} ()}.
     */
    public static void abort() {
        Transaction tx = getRequiredThreadLocalTransaction();
        tx.abort();
    }

    /**
     * Commits the Transaction in the ThreadLocalTransaction transaction.
     * <p/>
     * For more information see {@link Transaction#abort()} ()}.
     */
    public static void commit() {
        Transaction tx = getRequiredThreadLocalTransaction();
        tx.commit();
    }

    ///**
    // * Sets the timeout
    // * @param timeout
    // * @param timeoutTimeUnit
    // */
    //public static void setTimeoutNs(long timeout, TimeUnit timeoutTimeUnit) {
    //    Transaction tx = getRequiredThreadLocalTransaction();
    //    tx.setTimeoutNs(timeout, timeoutTimeUnit);
    //}

    /**
     * Scheduled an deferred task on the Transaction in the ThreadLocalTransaction. This task is executed after
     * the transaction commits and one of the use cases is starting transactions.
     * <p/>
     * For more information see {@link Transaction#registerLifecycleListener(TransactionLifecycleListener)}.
     *
     * @param task the deferred task to execute.
     * @throws NullPointerException if task is null.
     */
    public static void scheduleDeferredTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = getRequiredThreadLocalTransaction();
        tx.registerLifecycleListener(new TransactionLifecycleListener() {
            @Override
            public void notify(Transaction tx, TransactionLifecycleEvent event) {
                if (event == TransactionLifecycleEvent.postCommit) {
                    task.run();
                }
            }
        });
    }

    /**
     * Scheduled an compensating task on the Transaction in the ThreadLocalTransaction. This task is executed after
     * the transaction aborts and one of the use cases is cleaning up non transaction resources like the file system.
     * <p/>
     * For more information see {@link Transaction#registerLifecycleListener(TransactionLifecycleListener)}.
     *
     * @param task the deferred task to execute.
     * @throws NullPointerException if task is null.
     */
    public static void scheduleCompensatingTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = getRequiredThreadLocalTransaction();
        tx.registerLifecycleListener(new TransactionLifecycleListener() {
            @Override
            public void notify(Transaction tx, TransactionLifecycleEvent event) {
                if (event == TransactionLifecycleEvent.postAbort) {
                    task.run();
                }
            }
        });
    }

    //we don't want instances

    private StmUtils() {
    }
}
