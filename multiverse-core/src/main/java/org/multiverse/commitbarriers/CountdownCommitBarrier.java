package org.multiverse.commitbarriers;

import org.multiverse.api.Transaction;
import org.multiverse.utils.TodoException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * A CountdownCommitBarrier is a blocking structure (a latch) that commits or aborts bases on the number of
 * waiting transactions. Once all the members of the barrier have returned, all the transactions are committed.
 * <p/>
 * This functionality is useful for two phase commit related functionality.
 * <p/>
 * The CountdownCommitBarrier can't be reused.
 * <p/>
 * A CountdownCommitBarrier is thread-safe to use.
 *
 * @author Peter Veentjer.
 * @see VetoCommitBarrier
 */
public final class CountdownCommitBarrier extends AbstractCommitBarrier {

    private final int parties;

    /**
     * Create a new CountdownCommitBarrier that uses an unfair lock.
     *
     * @param parties the number of parties waiting. If the number of parties is 0, the VetoCommitBarrier is created
     *                committed, else it will be closed.
     * @throws IllegalArgumentException if parties is smaller than 0.
     */
    public CountdownCommitBarrier(int parties) {
        this(parties, false);
    }

    /**
     * Creates a new CountdownCommitBarrier.
     *
     * @param parties the number of parties waiting. If the number of parties is 0, the VetoCommitBarrier is created
     *                committed, else it will be closed.
     * @param fair    if the lock bu this CountdownCommitBarrier is fair.
     * @throws IllegalArgumentException if parties smaller than 0.
     */
    public CountdownCommitBarrier(int parties, boolean fair) {
        super(parties == 0 ? Status.committed : Status.closed, fair);

        if (parties < 0) {
            throw new IllegalArgumentException();
        }

        this.parties = parties;
    }

    /**
     * Returns the number of parties that want to join this CountdownCommitBarrier.
     *
     * @return the number of parties.
     */
    public int getParties() {
        return parties;
    }

    private boolean isLastParty() {
        return getNumberWaiting() == parties - 1;
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
     * @throws NullPointerException         if tx is null.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                      if tx already is committed or aborted.
     * @throws ClosedCommitBarrierException if commitGroup already aborted or committed.
     */
    public void awaitCommitUninterruptibly(Transaction tx) {
        ensureNotDead(tx);

        List<Runnable> postCommitTasks = null;
        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();

                    if (isLastParty()) {
                        postCommitTasks = signalCommit();
                    } else {
                        addWaiter(tx);

                        while (getStatus() == Status.closed) {
                            statusCondition.awaitUninterruptibly();
                        }
                    }
                    break;
                case aborted:
                    tx.abort();

                    String abortedMsg = format("Can't call awaitCommitUninterruptible on already aborted " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new ClosedCommitBarrierException(abortedMsg);
                case committed:
                    tx.abort();

                    String commitMsg = format("Can't call awaitCommitUninterruptible on already committed " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new ClosedCommitBarrierException(commitMsg);
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
     * Returns one party and awaits commit.
     *
     * @throws InterruptedException         if the waiting thread is interrupted. The CountdownCommitBarrier also is aborted.
     * @throws ClosedCommitBarrierException if this CountdownCommitBarrier already is committed or aborted.
     */
    public void awaitCommit() throws InterruptedException {
        try {
            lock.lockInterruptibly();

            List<Runnable> postCommitTasks = null;
            try {
                switch (getStatus()) {
                    case closed:
                        if (isLastParty()) {
                            postCommitTasks = signalCommit();
                        } else {
                            addWaiter(null);
                            while (getStatus() == Status.closed) {
                                statusCondition.await();
                            }
                        }
                        break;
                    case aborted:
                        String abortedMsg = format("Can't call awaitCommit on already aborted CountdownCommitBarrier");
                        throw new ClosedCommitBarrierException(abortedMsg);
                    case committed:
                        String commitMsg = format("Can't call awaitCommit on already committed CountdownCommitBarrier");
                        throw new ClosedCommitBarrierException(commitMsg);
                    default:
                        throw new IllegalStateException();
                }
            } finally {
                lock.unlock();
            }

            if (!isCommitted()) {
                throw new IllegalStateException();
            }

            executeTasks(postCommitTasks);
        } catch (InterruptedException ex) {
            abort();
            throw ex;
        }
    }

    /**
     * Returns one party and awaits commit. This method only blocks for a very short amount of time.
     * <p/>
     * If the CountdownCommitBarrier already is aborted or committed, the transaction is aborted.
     *
     * @param tx the Transaction that wants to join the other parties to commit with.
     * @return true if CountdownCommitBarrier was committed, false if aborted.
     * @throws ClosedCommitBarrierException if tx or this CountdownCommitBarrier is aborted or committed.
     * @throws NullPointerException         if tx is null.
     */
    public boolean tryAwaitCommit(Transaction tx) {
        ensureNotDead(tx);

        try {
            if (!lock.tryLock()) {
                abort();
                return false;
            }

            List<Runnable> postCommitTasks = null;
            try {
                switch (getStatus()) {
                    case closed:
                        tx.prepare();

                        if (isLastParty()) {
                            postCommitTasks = signalCommit();
                        } else {
                            postCommitTasks = signalAborted();
                        }
                        break;
                    case aborted:
                        String abortedMsg = format("Can't call tryAwaitCommit on already aborted " +
                                "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                        throw new ClosedCommitBarrierException(abortedMsg);
                    case committed:
                        String commitMsg = format("Can't call tryAwaitCommit on already committed " +
                                "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                        throw new ClosedCommitBarrierException(commitMsg);
                    default:
                        throw new IllegalStateException();
                }
            } finally {
                lock.unlock();
            }

            executeTasks(postCommitTasks);
            return isCommitted();
        } finally {
            finish(tx);
        }
    }


    /**
     * If a timeout occurs, the CountdownCommitBarrier is aborted.
     * <p/>
     * If the CountdownCommitBarrier already is aborted or committed, the transaction is aborted.
     *
     * @param tx      the transaction used by the party.
     * @param timeout time maximum amount of time to wait till this CountdownCommitBarrier is committed.
     * @param unit    the TimeUnit for the timeout argument
     * @return true if this CountdownCommitBarrier was committed, false otherwise.
     * @throws NullPointerException         if tx or unit is null.
     * @throws ClosedCommitBarrierException if this CountdownCommitBarrier already is aborted or committed.
     */
    public boolean tryAwaitCommit(Transaction tx, long timeout, TimeUnit unit) {
        ensureNotDead(tx);

        if (unit == null) {
            throw new NullPointerException();
        }

        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();
                    if (isLastParty()) {

                    } else {

                    }
                    long timeoutNs = unit.toNanos(timeout);

                    throw new TodoException();
                case aborted:
                    String abortedMsg = format("Can't call tryAwaitCommit on already aborted " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new ClosedCommitBarrierException(abortedMsg);
                case committed:
                    String commitMsg = format("Can't call tryAwaitCommit on already committed " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new ClosedCommitBarrierException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns one party and awaits commit.
     * <p/>
     * If the CountdownCommitBarrier already is aborted or committed, the transaction is aborted.
     *
     * @param tx the transaction that belongs to the party.
     * @throws InterruptedException         if the thread is interrupted while waiting.
     * @throws ClosedCommitBarrierException if this CountdownCommitBarrier already is aborted or committed.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                      if tx already is aborted or committed.
     * @throws NullPointerException         if tx is null.
     */
    public void awaitCommit(Transaction tx) throws InterruptedException {
        ensureNotDead(tx);

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException ex) {
            abort();
            tx.abort();
            throw ex;
        }

        List<Runnable> postCommitTasks = null;
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();
                    if (isLastParty()) {
                        postCommitTasks = signalCommit();
                    } else {
                        addWaiter(tx);
                        while (getStatus() == Status.closed) {
                            try {
                                statusCondition.await();
                            } catch (InterruptedException ex) {
                                abort();
                                tx.abort();
                                throw ex;
                            }
                        }
                    }
                    break;
                case aborted:
                    String abortedMsg = format("Can't call awaitCommit on already aborted " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new ClosedCommitBarrierException(abortedMsg);
                case committed:
                    String commitMsg = format("Can't call awaitCommit on already committed " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new ClosedCommitBarrierException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        finish(tx);

        executeTasks(postCommitTasks);
    }
}
