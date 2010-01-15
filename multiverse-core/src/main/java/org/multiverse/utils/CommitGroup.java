package org.multiverse.utils;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The CommitGroup is a synchronization primitive that makes it possible to execute a 2 phase commit;
 * so all transaction within a CommitGroup commit, or they all abort.
 *
 * @author Peter Veentjer.
 */
public final class CommitGroup {

    private volatile Status status = Status.open;

    private final Lock lock = new ReentrantLock();
    private final Condition prepared = lock.newCondition();
    private volatile int preparedCount = 0;

    /**
     * Returns the number of Transactions that have prepared and are waiting to commit. Value is 0
     * after commit or abort.
     *
     * @return the number of transactions prepared.
     */
    public int getPreparedCount() {
        return preparedCount;
    }

    /**
     * Checks if this CommitGroup is open.
     *
     * @return true if open, false otherwise.
     */
    public boolean isOpen() {
        return status == Status.open;
    }

    /**
     * Checks if this CommitGroup already is committed.
     *
     * @return true if committed, false otherwise.
     */
    public boolean isCommitted() {
        return status == Status.committed;
    }

    /**
     * Checks if this CommitGroup already is aborted.
     *
     * @return true if aborted, false otherwise.
     */
    public boolean isAborted() {
        return status == Status.aborted;
    }

    /**
     * Signals to all prepared Transactions in this CommitGroup that they should commit.
     * <p/>
     * If the CommitGroup already is committed, this call is ignored.
     *
     * @throws IllegalStateException if the CommitGroup already is aborted.
     */
    public void commit() {
        lock.lock();
        try {
            switch (status) {
                case open:
                    status = Status.committed;
                    prepared.signalAll();
                    break;
                case aborted:
                    throw new IllegalStateException();
                case committed:
                    return;
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Signals to all prepared Transactions in this CommitGroup that they should abort.
     * <p/>
     * If the CommitGroup already is aborted, this call is ignored.
     *
     * @throws IllegalStateException if the CommitGroup already is committed.
     */
    public void abort() {
        lock.lock();
        try {
            switch (status) {
                case open:
                    preparedCount = 0;
                    status = Status.aborted;
                    prepared.signalAll();
                    break;
                case aborted:
                    return;
                case committed:
                    throw new IllegalStateException();
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Commits this Transaction and all other transactions in the commitgroup that have prepared.
     *
     * @param tx the Transaction to commit.
     * @throws NullPointerException     if tx is null.
     * @throws DeadTransactionException if the Transaction already is aborted or committed.
     * @throws org.multiverse.api.exceptions.CommitFailureException
     *                                  if the commit was not executed successfully.
     * @throws IllegalStateException    if the CommitGroup isn't open anymore.
     */
    public void commit(Transaction tx) {
        ensureNotDead(tx);

        lock.lock();
        try {
            if (this.status != Status.open) {
                throw new IllegalStateException();
            }

            tx.prepare();
            status = Status.committed;
            preparedCount = 0;
            prepared.signalAll();
        } finally {
            lock.unlock();
        }

        tx.commit();
    }

    private static void ensureNotDead(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        TransactionStatus status = tx.getStatus();
        if (status != TransactionStatus.active && status != TransactionStatus.prepared) {
            throw new DeadTransactionException();
        }
    }

    /*
    public void awaitCommit()throws InterruptedException{
    }

    public void awaitCommitUninterruptible(){
    }

    public void tryAwaitCommit(long timeunit, TimeUnit unit)throws InterruptedException{
    } */


    /**
     * Awaits for the tx to commit. It will commit when all transactions on the group are going to commit.
     *
     * @param tx the Transaction to commit.
     * @throws InterruptedException     if the thread is interrupted
     * @throws NullPointerException     if tx is null.
     * @throws DeadTransactionException if tx is committed/aborted.
     * @throws IllegalStateException    if the commitgroup isn't open anymore.
     */
    public void awaitCommit(Transaction tx) throws InterruptedException {
        ensureNotDead(tx);

        lock.lock();
        try {
            if (status != Status.open) {
                throw new IllegalStateException();
            }

            tx.prepare();
            preparedCount++;
            while (status == Status.open) {
                prepared.await();
            }
        } finally {
            lock.unlock();
        }

        //todo: the the thread is interrupted, tx is not aborted.
        if (isCommitted()) {
            tx.commit();
        } else {
            tx.abort();
        }
    }

    public void awaitCommitUninterruptible(Transaction tx) {
        ensureNotDead(tx);

        lock.lock();
        try {
            if (status != Status.open) {
                throw new IllegalStateException();
            }

            tx.prepare();

            while (status == Status.open) {
                prepared.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }

        if (isCommitted()) {
            tx.commit();
        } else {
            tx.abort();
        }
    }


    /*
    public boolean awaitCommit(Transaction tx, long timeout, TimeUnit unit) throws InterruptedException {
        ensureNotDead(tx);

        long timeoutNs = unit.toNanos(timeout);
        if (timeoutNs < 0) {
            return false;
        }

        lock.lock();
        try {
            if (status != Status.open) {
                throw new IllegalStateException();
            }

            tx.prepare();

            while (status == Status.open) {
                prepared.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }

        if (isCommitted()) {
            tx.commit();
        } else {
            tx.abort();
        }
    }  */

    enum Status {
        open, committed, aborted
    }
}
