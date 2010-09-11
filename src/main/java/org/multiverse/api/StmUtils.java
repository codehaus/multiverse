package org.multiverse.api;

import org.multiverse.api.exceptions.Retry;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.api.references.*;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;

/**
 * A utility class with convenience methods to access the {@link org.multiverse.api.Stm} or
 * {@link Transaction}. These methods can be imported using the static import for a less
 * ugly (but potentially more confusing) syntax.
 *
 * @author Peter Veentjer.
 */
public class StmUtils {

    private final static RefFactory refFactory = getGlobalStmInstance().getDefaultRefFactory();

    /**
     * Creates a committed IntRef with the provided value.
     *
     * @param value the initial value of the IntRef
     * @return the created IntRef.
     */
    public static IntRef newIntRef(int value) {
        return refFactory.newIntRef(value);
    }

    /**
     * Creates a committed IntRef with 0 as initial value.
     *
     * @return the created IntRef.
     */
    public static IntRef newIntRef() {
        return refFactory.newIntRef(0);
    }

    /**
     * Creates a committed LongRef with 0 as initial value.
     *
     * @return the created LongRef.
     */
    public static LongRef newLongRef() {
        return refFactory.newLongRef(0);
    }

    /**
     * Creates a committed LongRef with the provided value.
     *
     * @param value the initial value of the LongRef.
     * @return the created LongRef.
     */
    public static LongRef newLongRef(long value) {
        return refFactory.newLongRef(value);
    }

    /**
     * Creates a committed DoubleRef with 0 as initial value.
     *
     * @return the created DoubleRef.
     */
    public static DoubleRef newDoubleRef() {
        return refFactory.newDoubleRef(0);
    }

    /**
     * Creates a committed DoubleRef with the provided value.
     *
     * @param value the initial value.
     * @return the created DoubleRef.
     */
    public static DoubleRef newDoubleRef(double value) {
        return refFactory.newDoubleRef(value);
    }

    /**
     * Creates a committed BooleanRef with false as initial value.
     *
     * @return the created BooleanRef.
     */
    public static BooleanRef newBooleanRef() {
        return refFactory.newBooleanRef(false);
    }

    /**
     * Creates a committed BooleanRef with the provided value.
     *
     * @param value the initial value
     * @return the created BooleanRef.
     */
    public static BooleanRef newBooleanRef(boolean value) {
        return refFactory.newBooleanRef(value);
    }

    /**
     * Creates a committed Ref with null as initial value.
     *
     * @param <E> the type of the Ref.
     * @return the created Ref.
     */
    public static <E> Ref<E> newRef() {
        return refFactory.newRef(null);
    }

    /**
     * Creates a committed Ref with the provided value.
     *
     * @param value the initial value of the Ref.
     * @param <E>   the type of the Ref.
     * @return the created Ref.
     */
    public static <E> Ref<E> newRef(E value) {
        return refFactory.newRef(value);
    }

    /**
     * Does a retry. This behavior is needed for blocking transactions; transaction that wait for a state change
     * to happen on certain datastructures, e.g. an item to come available on a transactional blocking queue.
     * <p/>
     * Under the hood the retry throws an Retry that will be caught up the callstack
     * (by the {@link AtomicBlock for example). The Retry should not be caught by user code in 99% procent
     * of the cases.
     */
    public static void retry() {
        throw Retry.INSTANCE;
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

    /**
     * Scheduled an deferred or compensating task on the Transaction in the ThreadLocalTransaction. This task is
     * executed after the transaction commits or aborts.
     * <p/>
     * For more information see {@link Transaction#register(org.multiverse.api.lifecycle.TransactionLifecycleListener)} (org.multiverse.api.lifecycle.TransactionLifecycleListener)}.
     *
     * @param task the deferred task to execute.
     * @throws NullPointerException if task is null.
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException
     *                              if no Transaction is getAndSet at the
     *                              {@link org.multiverse.api.ThreadLocalTransaction}.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state to accept a compensating or deferred task.
     */
    public static void scheduleCompensatingOrDeferredTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = getRequiredThreadLocalTransaction();
        tx.register(new TransactionLifecycleListener() {
            @Override
            public void notify(Transaction tx, TransactionLifecycleEvent event) {
                if (event == TransactionLifecycleEvent.PostCommit
                        || event == TransactionLifecycleEvent.PostAbort) {
                    task.run();
                }
            }
        });
    }

    /**
     * Scheduled an deferred task on the Transaction in the ThreadLocalTransaction. This task is executed after
     * the transaction commits and one of the use cases is starting transactions.
     * <p/>
     * For more information see {@link Transaction#register(org.multiverse.api.lifecycle.TransactionLifecycleListener)} (org.multiverse.api.lifecycle.TransactionLifecycleListener)}.
     *
     * @param task the deferred task to execute.
     * @throws NullPointerException if task is null.
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException
     *                              if no Transaction is getAndSet at the
     *                              {@link org.multiverse.api.ThreadLocalTransaction}.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state to accept a deferred task.
     */
    public static void scheduleDeferredTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = getRequiredThreadLocalTransaction();
        tx.register(new TransactionLifecycleListener() {
            @Override
            public void notify(Transaction tx, TransactionLifecycleEvent event) {
                if (event == TransactionLifecycleEvent.PostCommit) {
                    task.run();
                }
            }
        });
    }

    /**
     * Scheduled an compensating task on the Transaction in the ThreadLocalTransaction. This task is executed after
     * the transaction aborts and one of the use cases is cleaning up non transaction resources like the file system.
     * <p/>
     * For more information see {@link Transaction#register(org.multiverse.api.lifecycle.TransactionLifecycleListener)} (TransactionLifecycleListener)}.
     *
     * @param task the deferred task to execute.
     * @throws NullPointerException if task is null.
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException
     *                              if no Transaction is getAndSet at the
     *                              {@link org.multiverse.api.ThreadLocalTransaction}.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state to accept a compensating task.
     */
    public static void scheduleCompensatingTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = getRequiredThreadLocalTransaction();
        tx.register(new TransactionLifecycleListener() {
            @Override
            public void notify(Transaction tx, TransactionLifecycleEvent event) {
                if (event == TransactionLifecycleEvent.PostAbort) {
                    task.run();
                }
            }
        });
    }

    //we don't want instances

    private StmUtils() {
    }
}
