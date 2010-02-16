package org.multiverse.commitbarriers;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.unmodifiableList;

/**
 * @author Peter Veentjer.
 */
abstract class AbstractCommitBarrier {

    private final static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

    protected final Lock lock;
    protected final Condition statusCondition;

    private final List<Transaction> waitingTransactions = new LinkedList<Transaction>();

    private volatile Status status;
    private volatile int numberWaiting = 0;

    //for all non final non volatile variables; they only should be accessed while the lock is acquired.
    private List<Runnable> onAbortTasks;
    private List<Runnable> onCommitTasks;

    public AbstractCommitBarrier(Status status, boolean fair) {
        if (status == null) {
            throw new NullPointerException();
        }
        this.status = status;
        this.lock = new ReentrantLock(fair);
        this.statusCondition = lock.newCondition();
    }

    protected final Status getStatus() {
        return status;
    }

    /**
     * Returns the number of Transactions that have prepared and are waiting to commit. Value eventually
     * becomes null after a commit or abort.
     *
     * @return the number of transactions prepared.
     */
    public final int getNumberWaiting() {
        return numberWaiting;
    }

    /**
     * Checks if this VetoCommitBarrier is closed. This is the initial status of the barrier.
     *
     * @return true if closed, false otherwise.
     */
    public final boolean isClosed() {
        return status == Status.closed;
    }

    /**
     * Checks if this VetoCommitBarrier already is committed.
     *
     * @return true if committed, false otherwise.
     */
    public final boolean isCommitted() {
        return status == Status.committed;
    }

    /**
     * Checks if this VetoCommitBarrier already is aborted.
     *
     * @return true if aborted, false otherwise.
     */
    public final boolean isAborted() {
        return status == Status.aborted;
    }

    /**
     * Returns an immutable list of all transactions that currently are waiting for this barrier to commit.
     *
     * @return list of all transactions currently waiting.
     */
    public final List<Transaction> getWaitingTransaction() {
        lock.lock();
        try {
            return unmodifiableList(new LinkedList<Transaction>(waitingTransactions));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Only should be made when the lock is acquired.
     *
     * @return
     */
    protected final List<Runnable> signalCommit() {
        numberWaiting = 0;
        status = Status.committed;
        statusCondition.signalAll();
        onAbortTasks = null;
        List<Runnable> result = onCommitTasks;
        onCommitTasks = null;
        return result;
    }

    /**
     * Only should be made when the lock is acquired.
     *
     * @return
     */
    protected final List<Runnable> signalAborted() {
        numberWaiting = 0;
        status = Status.aborted;
        statusCondition.signalAll();
        onCommitTasks = null;
        List<Runnable> result = onAbortTasks;
        onAbortTasks = null;
        return result;
    }

    /**
     * Aborts this CommitBarrier. If there are any prepared transactions that are waiting for this CommitBarrier
     * to complete, they are aborted.
     * <p/>
     * If the CommitBarrier already is aborted, this call is ignored.
     *
     * @throws ClosedCommitBarrierException if the VetoCommitBarrier already is committed.
     */
    public final void abort() {
        List<Runnable> postAbortTasks = null;
        lock.lock();
        try {
            switch (status) {
                case closed:
                    postAbortTasks = signalAborted();
                    break;
                case aborted:
                    return;
                case committed:
                    String commitMsg = "Can't abort already committed CommitBarrier";
                    throw new ClosedCommitBarrierException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        executeTasks(postAbortTasks);
    }

    /**
     * Executes the tasks. Can be called with a null argument.
     *
     * @param tasks the tasks to execute.
     */
    protected static void executeTasks(List<Runnable> tasks) {
        if (tasks == null) {
            return;
        }

        for (Runnable task : tasks) {
            task.run();
        }
    }

    /**
     * Awaits for this barrier to open (commit or abort).
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting.
     */
    public final void awaitOpen() throws InterruptedException {
        if (status != Status.closed) {
            return;
        }

        lock.lockInterruptibly();
        try {
            while (status == Status.closed) {
                statusCondition.await();
            }
        } finally {
            lock.unlock();
        }

    }

    /**
     * Awaits for this barrier to open (commit or abort).
     * <p/>
     * This call is not responsive to interrupts.
     */
    public final void awaitOpenUninterruptibly() {
        if (status == Status.closed) {
            lock.lock();
            try {
                while (status == Status.closed) {
                    statusCondition.awaitUninterruptibly();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Waits for this barrier to open (abort or commit).
     *
     * @param timeout the maximum amount of time to wait for the barrier to close.
     * @param unit    the TimeUnit for the timeout argument.
     * @return true if the wait was a success, false if the barrier still is closed.
     * @throws InterruptedException if the thread is interrupted while waiting.
     * @throws NullPointerException if unit is null.
     */
    public final boolean tryAwaitOpen(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (status == Status.closed) {
            long timeoutNs = unit.toNanos(timeout);

            lock.lockInterruptibly();
            try {
                while (status == Status.closed) {
                    timeoutNs = statusCondition.awaitNanos(timeoutNs);
                    if (timeoutNs <= 0) {
                        return false;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        return true;
    }

    /**
     * Tries to await the close of the barrier.
     * <p/>
     * This call is not responsive to interrupts.
     *
     * @param timeout the maximum amount of time to wait for the barrier to be closed.
     * @param unit    the timeunit for the timeout argument.
     * @return true if the wait was a success, false otherwise.
     */
    public final boolean tryAwaitOpenUninterruptibly(long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (status == Status.closed) {
            long timeoutNs = unit.toNanos(timeout);
            lock.lock();
            try {
                while (status == Status.closed) {
                    timeoutNs = awaitNanosUninterruptible(timeoutNs);
                    if (timeoutNs <= 0) {
                        return false;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        return true;
    }

    private long awaitNanosUninterruptible(long timeoutNs) {
        boolean restoreInterrupt = Thread.interrupted();

        try {
            while (true) {
                long startNs = System.nanoTime();
                try {
                    return statusCondition.awaitNanos(timeoutNs);
                } catch (InterruptedException ex) {
                    timeoutNs -= (System.nanoTime() - startNs);
                    restoreInterrupt = true;
                }
            }
        } finally {
            //restore interrupt if needed
            if (restoreInterrupt) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Sets the timeout on this AbstractCommitBarrier. If the barrier hasn't committed/aborted before the timeout
     * it automatically is aborted. This is a function that typically is used when initializing the CommitBarrier.
     * <p/>
     * The timeout starts running when this method is called.
     *
     * @param timeout the maximum amount of time this barrier is allowed to run.
     * @param unit    the TimeUnit of the timeout parameter.
     * @throws NullPointerException         if unit is null.
     * @throws ClosedCommitBarrierException if the CommitBarrier already is aborted or committed.
     */
    public final void setTimeout(long timeout, TimeUnit unit) {
        lock.lock();
        try {
            switch (status) {
                case closed:
                    Runnable command = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                abort();
                            } catch (IllegalStateException ignore) {
                            }
                        }
                    };
                    executor.schedule(command, timeout, unit);
                    break;
                case committed:
                    String commitMsg = "Can't set a timeout on an already commit CommitBarrier.";
                    throw new ClosedCommitBarrierException(commitMsg);
                case aborted:
                    String abortMsg = "Can't set a timeout on an already aborted CommitBarrier.";
                    throw new ClosedCommitBarrierException(abortMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registers a task that is executed once the CommitBarrier aborts.
     * <p/>
     * The task will be executed after the abort and will be executed by the thread that does the actual abort.
     * <p/>
     * The tasks will be executed in the order they are registered and will be executed at most once. If one of the
     * tasks throws a RuntimeException, the following will not be executed.
     *
     * @param task the task that is executed once the CommitBarrier commits.
     * @throws NullPointerException         if task is null.
     * @throws ClosedCommitBarrierException if this CommitBarrier already is aborted or committed.
     */
    public final void registerOnAbortTask(Runnable task) {
        lock.lock();
        try {
            switch (status) {
                case closed:
                    if (task == null) {
                        throw new NullPointerException();
                    }

                    if (onAbortTasks == null) {
                        onAbortTasks = new LinkedList<Runnable>();
                    }

                    onAbortTasks.add(task);
                    break;
                case committed:
                    String commitMsg = "Can't register on abort task on already committed CommitBarrier";
                    throw new ClosedCommitBarrierException(commitMsg);
                case aborted:
                    String abortMsg = "Can't register on abort task on already aborted CommitBarrier";
                    throw new ClosedCommitBarrierException(abortMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registers a task that is executed once the CommitBarrier commits.
     * <p/>
     * The task will be executed after the commit and will be executed by the thread that does the actual commit.
     * <p/>
     * The tasks will be executed in the order they are registered and will be executed at most once. If one of the
     * tasks throws a RuntimeException, the following will not be executed.
     *
     * @param task the task that is executed once the CommitBarrier commits.
     * @throws NullPointerException         if task is null.
     * @throws ClosedCommitBarrierException if this CommitBarrier already is aborted or committed.
     */
    public final void registerOnCommitTask(Runnable task) {
        lock.lock();
        try {
            switch (status) {
                case closed:
                    if (task == null) {
                        throw new NullPointerException();
                    }

                    if (onCommitTasks == null) {
                        onCommitTasks = new LinkedList<Runnable>();
                    }

                    onCommitTasks.add(task);
                    break;
                case committed:
                    String commitMsg = "Can't register on commit task on already committed CommitBarrier";
                    throw new ClosedCommitBarrierException(commitMsg);
                case aborted:
                    String abortMsg = "Can't register on commit task on already aborted CommitBarrier";
                    throw new ClosedCommitBarrierException(abortMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a Transaction to the set of waiters.
     * <p/>
     * Should only be called when the main lock is acquired.
     *
     * @param tx the transaction to add as waiter.
     * @throws NullPointerException  if tx is null.
     * @throws IllegalStateException if the transaction isn't closed.
     */
    protected final void addWaiter(Transaction tx) {
        if (status != Status.closed) {
            throw new IllegalStateException();
        }
        if (tx != null) {
            waitingTransactions.add(tx);
        }
        numberWaiting++;
    }

    /**
     * Finishes a Transaction.
     * <p/>
     * Can be called without the mainlock is acquired.
     *
     * @param tx
     * @throws NullPointerException if tx is null.
     */
    protected final void finish(Transaction tx) {
        if (tx == null) {
            return;
        }

        if (isCommitted()) {
            tx.commit();
        } else if (isAborted()) {
            tx.abort();
            //todo: better message
            throw new IllegalStateException();
        }
    }

    /**
     * Can be called without the mainlock is acquired.
     *
     * @param tx
     * @throws NullPointerException if tx is null.
     */
    protected final void ensureNotDead(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        TransactionStatus status = tx.getStatus();
        if (status != TransactionStatus.active && status != TransactionStatus.prepared) {
            throw new DeadTransactionException();
        }
    }

    enum Status {
        closed, committed, aborted
    }
}
