package org.multiverse.api;

import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.api.exceptions.RetryError;

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
     * Under the hood the retry throws an RetryError that will be caught up the chain
     * (by the TransactionTemplate for example).
     *
     * @throws NoTransactionFoundException if no transaction is found in the ThreadLocalTransaction.
     */
    public static void retry() {
        getRequiredThreadLocalTransaction();
        throw RetryError.create();
    }

    /**
     * Schedules a tasks so that it executes when the transaction commits.
     * <p/>
     * <p/>
     * See {@link Transaction#register(TransactionLifecycleListener task)}
     *
     * @param task the task that is executed when the transaction commits.
     * @throws NoTransactionFoundException if no transaction is found in the ThreadLocalTransaction.
     */
    public static void deferredExecute(Runnable task) {
        Transaction t = getRequiredThreadLocalTransaction();
        t.register(new TransactionListener(TransactionLifecycleEvent.postCommit, task));
    }

    /**
     * Schedules a tasks so that it executes when the transaction aborts.
     * <p/>
     * See {@link Transaction#register(TransactionLifecycleListener task)}
     *
     * @param task the task that is executed when the transaction commits.
     * @throws NoTransactionFoundException if no transaction is found in the ThreadLocalTransaction.
     */
    public static void compensatingExecute(Runnable task) {
        Transaction t = getRequiredThreadLocalTransaction();
        t.register(new TransactionListener(TransactionLifecycleEvent.postAbort, task));
    }

    public static class TransactionListener extends TransactionLifecycleListener {

        private final TransactionLifecycleEvent expectedEvent;
        private final Runnable task;


        public TransactionListener(TransactionLifecycleEvent expectedEvent, Runnable task) {
            this.expectedEvent = expectedEvent;
            this.task = task;
        }

        @Override
        public void notify(Transaction t, TransactionLifecycleEvent event) {
            if (expectedEvent == event) {
                task.run();
            }
        }
    }

    //we don't want instances.
    private StmUtils() {
    }
}
