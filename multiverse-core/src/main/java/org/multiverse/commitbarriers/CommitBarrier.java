package org.multiverse.commitbarriers;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.StandardThreadFactory;
import org.multiverse.utils.TodoException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

/**
 * @author Peter Veentjer.
 */
public abstract class CommitBarrier {

    private final static ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(
            5, new StandardThreadFactory(Thread.NORM_PRIORITY, true));

    private volatile ScheduledExecutorService executorService = EXECUTOR;
    protected final Lock lock;
    protected final Condition statusCondition;

    private final List<Transaction> waitingTransactions = new LinkedList<Transaction>();

    private volatile Status status;
    private volatile int numberWaiting = 0;

    //for all non final non volatile variables; they only should be accessed while the lock is acquired.
    private List<Runnable> onAbortTasks;
    private List<Runnable> onCommitTasks;

    public CommitBarrier(Status status, boolean fair) {
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
     * <p/>
     * This method can be called no matter the state of the CommitBarrier.
     *
     * @return list of all transactions currently waiting.
     */
    public final List<Transaction> getWaitingTransactions() {
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
     * @return the List of onCommitTasks that needs to be executed (is allowed to be null).
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
     * @return the List of onAbortTasks that needs to be executed (is allowed to be null).
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
     * @throws CommitBarrierOpenException if the VetoCommitBarrier already is committed.
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
                    throw new CommitBarrierOpenException(commitMsg);
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
     * Sets the ScheduledExecutorService to be used by this CommitBarrier for the timeout. This method can always
     * be called no matter the state of the CommitBarrier.
     *
     * @param executorService the ScheduledExecutorService this CommitBarrier is going to use for timeout.
     * @throws NullPointerException if executorService  is null.
     */
    public void setScheduledExecutorService(ScheduledExecutorService executorService) {
        if (executorService == null) {
            throw new NullPointerException();
        }
        this.executorService = executorService;
    }

    /**
     * Sets the timeout on this CommitBarrier. If the barrier hasn't committed/aborted before the timeout
     * it automatically is aborted. This is a function that typically is used when initializing the CommitBarrier.
     * <p/>
     * The timeout starts running when this method is called.
     *
     * @param timeout the maximum amount of time this barrier is allowed to run.
     * @param unit    the TimeUnit of the timeout parameter.
     * @throws NullPointerException       if unit is null.
     * @throws CommitBarrierOpenException if the CommitBarrier already is aborted or committed.
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
                    executorService.schedule(command, timeout, unit);
                    break;
                case committed:
                    String commitMsg = "Can't set a timeout on an already commit CommitBarrier.";
                    throw new CommitBarrierOpenException(commitMsg);
                case aborted:
                    String abortMsg = "Can't set a timeout on an already aborted CommitBarrier.";
                    throw new CommitBarrierOpenException(abortMsg);
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
     * @throws NullPointerException       if task is null.
     * @throws CommitBarrierOpenException if this CommitBarrier already is aborted or committed.
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
                    throw new CommitBarrierOpenException(commitMsg);
                case aborted:
                    String abortMsg = "Can't register on abort task on already aborted CommitBarrier";
                    throw new CommitBarrierOpenException(abortMsg);
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
     * @throws NullPointerException       if task is null.
     * @throws CommitBarrierOpenException if this CommitBarrier already is aborted or committed.
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
                    throw new CommitBarrierOpenException(commitMsg);
                case aborted:
                    String abortMsg = "Can't register on commit task on already aborted CommitBarrier";
                    throw new CommitBarrierOpenException(abortMsg);
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
     * @param tx the transaction too finish
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
     * Ensures that a transaction is not dead.
     * <p/>
     * Can be called without the mainlock is acquired.
     *
     * @param tx the transaction to check.
     * @throws DeadTransactionException if tx is dead.
     * @throws NullPointerException     if tx is null.
     */
    protected final void ensureNotDead(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        TransactionStatus status = tx.getStatus();
        if (status.isDead()) {
            throw new DeadTransactionException();
        }
    }

    /**
     * Awaits for the tx to commit. It will commit when all transactions on the group are going to commit.
     * <p/>
     * If the VetoCommitBarrier already is aborted or committed, the transaction is aborted.
     * <p/>
     * This call is responsive to interrupts.
     *
     * @param tx the Transaction to commit.
     * @throws InterruptedException       if the thread is interrupted while waiting.
     * @throws NullPointerException       if tx is null.
     * @throws DeadTransactionException   if tx is committed/aborted.
     * @throws CommitBarrierOpenException if this VetoCommitBarrier is committed or aborted.
     */
    public void joinCommit(Transaction tx) throws InterruptedException {
        ensureNotDead(tx);

        List<Runnable> tasks = null;

        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();
                    addWaiter(tx);
                    if (isLastParty()) {
                        tasks = signalCommit();
                    } else {
                        while (getStatus() == Status.closed) {
                            try {
                                statusCondition.await();
                            } catch (InterruptedException ex) {
                                signalAborted();
                                tx.abort();
                                throw ex;
                            }
                        }
                    }
                    break;
                case committed:
                    String committedMsg = format("Can't await commit on already committed VetoCommitBarrier " +
                            "with transaction %s", tx.getConfig().getFamilyName());
                    throw new CommitBarrierOpenException(committedMsg);
                case aborted:
                    String abortMsg = format("Can't await commit on already aborted VetoCommitBarrier " +
                            "with transaction %s", tx.getConfig().getFamilyName());
                    throw new CommitBarrierOpenException(abortMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        //todo: the the thread is interrupted, tx is not aborted.
        finish(tx);

        executeTasks(tasks);
    }

    /**
     * Waits until all {@linkplain #getParties parties} have invoked <tt>await</tt> on this barrier.
     * <p/>
     * If the current thread is not the last to arrive then it is disabled for thread scheduling purposes and
     * lies dormant until one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>Some other thread times out while waiting for barrier
     * </ul>
     * <p/>
     * A transaction can be added that already is prepared. If the barrier already is committed or aborted, the
     * transaction is aborted.
     * <p/>
     * This call is not responsive to interrupts.
     *
     * @param tx the transaction
     * @throws NullPointerException       if tx is null.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                    if tx already is committed or aborted.
     * @throws CommitBarrierOpenException if commitGroup already aborted or committed.
     */

    public void joinCommitUninterruptibly(Transaction tx) {
        ensureNotDead(tx);

        List<Runnable> postCommitTasks = null;
        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();
                    addWaiter(tx);

                    if (isLastParty()) {
                        postCommitTasks = signalCommit();
                    } else {
                        while (getStatus() == Status.closed) {
                            statusCondition.awaitUninterruptibly();
                        }
                    }
                    break;
                case aborted:
                    tx.abort();

                    String abortedMsg = format("Can't call joinCommitUninterruptible on already aborted " +
                            "CountDownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new CommitBarrierOpenException(abortedMsg);
                case committed:
                    tx.abort();

                    String commitMsg = format("Can't call joinCommitUninterruptible on already committed " +
                            "CountDownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new CommitBarrierOpenException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        finish(tx);
        executeTasks(postCommitTasks);
    }

    /**
     * Returns one party and awaits commit. This method only blocks for a very short amount of time.
     * <p/>
     * If the CountDownCommitBarrier already is aborted or committed, the transaction is aborted.
     *
     * @param tx the Transaction that wants to join the other parties to commit with.
     * @return true if CountDownCommitBarrier was committed, false if aborted.
     * @throws CommitBarrierOpenException if tx or this CountDownCommitBarrier is aborted or committed.
     * @throws NullPointerException       if tx is null.
     */
    public boolean tryJoinCommit(Transaction tx) {
        ensureNotDead(tx);

        List<Runnable> postCommitTasks = null;
        boolean abort = true;
        lock.lock();
        try {
            try {
                switch (getStatus()) {
                    case closed:
                        tx.prepare();
                        addWaiter(tx);

                        if (isLastParty()) {
                            postCommitTasks = signalCommit();
                            abort = false;
                        } else {
                            postCommitTasks = signalAborted();
                        }
                        break;
                    case aborted:
                        String abortMsg = format("Can't call tryJoinCommit on already aborted " +
                                "CountDownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                        throw new CommitBarrierOpenException(abortMsg);
                    case committed:
                        String commitMsg = format("Can't call tryJoinCommit on already committed " +
                                "CountDownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                        throw new CommitBarrierOpenException(commitMsg);
                    default:
                        throw new IllegalStateException();
                }
            } finally {
                lock.unlock();
            }
        } finally {
            if (abort) {
                tx.abort();
            } else {
                tx.commit();
            }
        }

        executeTasks(postCommitTasks);
        return isCommitted();
    }


    public boolean tryJoinCommit(Transaction tx, long timeout, TimeUnit unit) throws InterruptedException {
        ensureNotDead(tx);

        ensureNotDead(tx);

        long timeoutNs = unit.toNanos(timeout);

        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();
                    addWaiter(tx);
                    while (getStatus() == Status.closed) {
                        try {
                            timeoutNs = statusCondition.awaitNanos(timeoutNs);
                            if (timeoutNs <= 0) {
                                signalAborted();
                                tx.abort();
                                return false;
                            }
                        } catch (InterruptedException ex) {
                            signalAborted();
                            tx.abort();
                            //for the time being.. needs to be replaced with a really uninterruptible version
                            throw ex;
                        }
                    }
                    break;
                case committed:
                    String commitMsg = "Can't await commit on an already committed VetoCommitBarrier";
                    throw new CommitBarrierOpenException(commitMsg);
                case aborted:
                    String abortMsg = "Can't await commit on an already aborted VetoCommitBarrier";
                    throw new CommitBarrierOpenException(abortMsg);
                default:
                    throw new NullPointerException();
            }
        } finally {
            lock.unlock();
        }

        finish(tx);
        return true;
    }

    public boolean tryJoinCommitUninterruptibly(Transaction tx, long timeout, TimeUnit unit) {
        ensureNotDead(tx);

        long timeoutNs = unit.toNanos(timeout);

        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();
                    addWaiter(tx);
                    while (getStatus() == Status.closed) {
                        try {
                            timeoutNs = statusCondition.awaitNanos(timeoutNs);
                            if (timeoutNs <= 0) {
                                signalAborted();
                                tx.abort();
                                return false;
                            }
                        } catch (InterruptedException ex) {
                            signalAborted();
                            tx.abort();
                            //for the time being.. needs to be replaced with a really uninterruptible version
                            throw new RuntimeException(ex);
                        }
                    }
                    break;
                case committed:
                    String commitMsg = "Can't await commit on an already committed VetoCommitBarrier";
                    throw new CommitBarrierOpenException(commitMsg);
                case aborted:
                    String abortMsg = "Can't await commit on an already aborted VetoCommitBarrier";
                    throw new CommitBarrierOpenException(abortMsg);
                default:
                    throw new NullPointerException();
            }
        } finally {
            lock.unlock();
        }

        finish(tx);

        throw new TodoException();
    }

    protected abstract boolean isLastParty();

    enum Status {
        closed, committed, aborted
    }
}