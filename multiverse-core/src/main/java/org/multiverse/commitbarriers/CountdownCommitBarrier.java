package org.multiverse.commitbarriers;

import org.multiverse.api.Transaction;
import org.multiverse.utils.TodoException;

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
     *                committed, else it will be open.
     * @throws IllegalArgumentException if parties smaller than 0.
     */
    public CountdownCommitBarrier(int parties) {
        this(parties, false);
    }

    /**
     * Creates a new CountdownCommitBarrier.
     *
     * @param parties the number of parties waiting. If the number of parties is 0, the VetoCommitBarrier is created
     *                committed, else it will be open.
     * @param fair    if the lock bu this CountdownCommitBarrier is fair.
     * @throws IllegalArgumentException if parties smaller than 0.
     */
    public CountdownCommitBarrier(int parties, boolean fair) {
        super(fair);

        if (parties < 0) {
            throw new IllegalArgumentException();
        }

        this.parties = parties;
        this.status = parties == 0 ? Status.committed : Status.open;
    }

    /**
     * Returns the number of parties that want to join this CountdownCommitBarrier.
     *
     * @return the number of parties.
     */
    public int getParties() {
        return parties;
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
     * A transaction can be added that already is prepared.
     * <p/>
     * This call is not responsive to interrupts.
     *
     * @param tx the transaction
     * @throws NullPointerException  if tx is null.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                               if tx already is committed or aborted.
     * @throws IllegalStateException if commitGroup already aborted or committed.
     */
    public void awaitCommitUninterruptibly(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        lock.lock();
        try {
            switch (status) {
                case open:
                    tx.prepare();

                    if (numberWaiting == parties - 1) {
                        status = Status.committed;
                        statusCondition.signalAll();
                    } else {
                        numberWaiting++;
                        while (status == Status.open) {
                            statusCondition.awaitUninterruptibly();
                        }
                        numberWaiting--;
                    }
                    break;
                case aborted:
                    String abortedMsg = format("Can't call awaitCommitUninterruptible on already aborted " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(abortedMsg);
                case committed:
                    String commitMsg = format("Can't call awaitCommitUninterruptible on already committed " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        after(tx);
    }

    private void after(Transaction tx) {
        if (isCommitted()) {
            tx.commit();
        } else {
            tx.abort();
            throw new IllegalStateException();
        }
    }

    /**
     * Returns one party and awaits commit.
     *
     * @throws InterruptedException  if the waiting thread is interrupted. The CountdownCommitBarrier also is aborted.
     * @throws IllegalStateException if this CountdownCommitBarrier already is committed or aborted.
     */
    public void awaitCommit() throws InterruptedException {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException ex) {
            abort();
            throw ex;
        }

        try {
            switch (status) {
                case open:
                    if (numberWaiting == parties - 1) {
                        status = Status.committed;
                        statusCondition.signalAll();
                    } else {
                        numberWaiting++;
                        try {
                            while (status == Status.open) {
                                try {
                                    statusCondition.await();
                                } catch (InterruptedException ex) {
                                    abort();
                                    throw ex;
                                }
                            }
                        } finally {
                            numberWaiting--;
                        }
                    }
                    break;
                case aborted:
                    String abortedMsg = format("Can't call awaitCommit on already aborted CountdownCommitBarrier");
                    throw new IllegalStateException(abortedMsg);
                case committed:
                    String commitMsg = format("Can't call awaitCommit on already committed CountdownCommitBarrier");
                    throw new IllegalStateException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        if (!isCommitted()) {
            throw new IllegalStateException();
        }
    }

    /**
     * Returns one party and awaits commit. This method only blocks for a very short amount of time.
     *
     * @param tx the Transaction that wants to join the other parties to commit with.
     * @return true if CountdownCommitBarrier was committed, false if aborted.
     * @throws IllegalStateException if tx or this CountdownCommitBarrier is aborted or committed.
     * @throws NullPointerException  if tx is null.
     */
    public boolean tryAwaitCommit(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (!lock.tryLock()) {
            return false;
        }

        try {
            switch (status) {
                case open:
                    switch (tx.getStatus()) {
                        case active:
                            tx.prepare();
                            break;
                        case prepared:
                            break;
                        default:
                            throw new IllegalStateException();//better exception.
                    }

                    if (numberWaiting == parties) {
                        status = Status.committed;
                        statusCondition.signalAll();
                        tx.commit();
                        return true;
                    } else {
                        status = Status.aborted;
                        statusCondition.signalAll();
                        tx.abort();
                        return false;
                    }
                case aborted:
                    String abortedMsg = format("Can't call tryAwaitCommit on already aborted " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(abortedMsg);
                case committed:
                    String commitMsg = format("Can't call tryAwaitCommit on already committed " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * If a timeout occurs, the CountdownCommitBarrier is aborted.
     *
     * @param tx      the transaction used by the party.
     * @param timeout time maximum amount of time to wait till this CountdownCommitBarrier is committed.
     * @param unit    the TimeUnit for the timeout argument
     * @return true if this CountdownCommitBarrier was committed, false otherwise.
     * @throws NullPointerException  if tx or unit is null.
     * @throws IllegalStateException if this CountdownCommitBarrier already is aborted or committed.
     */
    public boolean tryAwaitCommit(Transaction tx, long timeout, TimeUnit unit) {
        if (tx == null || unit == null) {
            throw new NullPointerException();
        }

        lock.lock();
        try {
            switch (status) {
                case open:
                    tx.prepare();
                    if (numberWaiting == parties - 1) {

                    } else {

                    }
                    long timeoutNs = unit.toNanos(timeout);


                    throw new TodoException();
                case aborted:
                    String abortedMsg = format("Can't call tryAwaitCommit on already aborted " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(abortedMsg);
                case committed:
                    String commitMsg = format("Can't call tryAwaitCommit on already committed " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns one party and awaits commit.
     *
     * @param tx the transaction that belongs to the party.
     * @throws InterruptedException  if the thread is interrupted while waiting.
     * @throws IllegalStateException if this CountdownCommitBarrier already is aborted or committed.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                               if tx already is aborted or committed.
     * @throws NullPointerException  if tx is null.
     */
    public void awaitCommit(Transaction tx) throws InterruptedException {
        if (tx == null) {
            throw new NullPointerException();
        }

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException ex) {
            abort();
            tx.abort();
            throw ex;
        }

        try {
            switch (status) {
                case open:
                    tx.prepare();
                    if (numberWaiting == parties - 1) {
                        status = Status.committed;
                        statusCondition.signalAll();
                    } else {
                        numberWaiting++;
                        while (status == Status.open) {
                            try {
                                statusCondition.await();
                            } catch (InterruptedException ex) {
                                abort();
                                tx.abort();
                                throw ex;
                            }
                        }
                        numberWaiting--;
                    }
                    break;
                case aborted:
                    String abortedMsg = format("Can't call awaitCommit on already aborted " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(abortedMsg);
                case committed:
                    String commitMsg = format("Can't call awaitCommit on already committed " +
                            "CountdownCommitBarrier with transaction %s ", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        after(tx);
    }
}
