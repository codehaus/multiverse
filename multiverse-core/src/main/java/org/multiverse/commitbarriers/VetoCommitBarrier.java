package org.multiverse.commitbarriers;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.TodoException;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * The VetoCommitBarrier is a synchronization primitive that makes it possible to execute a 2 phase commit;
 * so all transaction within a VetoCommitBarrier commit, or they all abort. The VetoCommitBarrier is useful if
 * there is a veto situation; so one transaction is able to commit or abort all others.
 * <p/>
 * This structure is thread-safe to use.
 * <p/>
 *
 * @author Peter Veentjer.
 */
public final class VetoCommitBarrier extends AbstractCommitBarrier {

    /**
     * Creates a new VetoCommitBarrier that uses an unfair Lock.
     */
    public VetoCommitBarrier() {
        this(false);
    }

    /**
     * Creates a new VetoCommitBarrier.
     *
     * @param fair if the lock should be fair.
     */
    public VetoCommitBarrier(boolean fair) {
        super(fair);
    }

    /**
     * Signals to all prepared Transactions in this VetoCommitBarrier that they should commit.
     * <p/>
     * If the VetoCommitBarrier already is committed, this call is ignored.
     *
     * @throws IllegalStateException if the VetoCommitBarrier already is aborted.
     */
    public void commit() {
        lock.lock();
        try {
            switch (status) {
                case open:
                    status = Status.committed;
                    statusCondition.signalAll();
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
     * Commits this Transaction and all other transactions in the commitgroup that have prepared.
     *
     * @param tx the Transaction to commit.
     * @throws NullPointerException     if tx is null.
     * @throws DeadTransactionException if the Transaction already is aborted or committed.
     * @throws org.multiverse.api.exceptions.CommitFailureException
     *                                  if the commit was not executed successfully.
     * @throws IllegalStateException    if the VetoCommitBarrier isn't open anymore.
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
            numberWaiting = 0;
            statusCondition.signalAll();
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

    /**
     * Awaits for the tx to commit. It will commit when all transactions on the group are going to commit.
     *
     * @param tx the Transaction to commit.
     * @throws InterruptedException     if the thread is interrupted while waiting.
     * @throws NullPointerException     if tx is null.
     * @throws DeadTransactionException if tx is committed/aborted.
     * @throws IllegalStateException    if the commitgroup isn't open anymore.
     */
    public void awaitCommit(Transaction tx) throws InterruptedException {
        if (tx == null) {
            throw new NullPointerException();
        }

        lock.lock();
        try {
            switch (status) {
                case open:
                    tx.prepare();

                    numberWaiting++;
                    while (status == Status.open) {
                        statusCondition.await();
                    }
                    numberWaiting--;
                    break;
                case committed:
                    String committedMsg = format("Can't await commit on already committed VetoCommitBarrier " +
                            "with transaction %s", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(committedMsg);
                case aborted:
                    String abortMsg = format("Can't await commit on already aborted VetoCommitBarrier " +
                            "with transaction %s", tx.getConfig().getFamilyName());
                    throw new IllegalStateException(abortMsg);
                default:
                    throw new IllegalStateException();
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
        if (tx == null) {
            throw new NullPointerException();
        }

        tx.prepare();

        lock.lock();
        try {
            numberWaiting++;
            if (status != Status.open) {
                throw new IllegalStateException();
            }

            while (status == Status.open) {
                statusCondition.awaitUninterruptibly();
            }
            numberWaiting--;
        } finally {
            lock.unlock();
        }

        if (isCommitted()) {
            tx.commit();
        } else {
            tx.abort();
        }
    }

    public boolean tryAwaitCommit(Transaction tx, long timeout, TimeUnit unit) throws InterruptedException {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (unit == null) {
            throw new NullPointerException();
        }

        throw new TodoException();
    }

    public boolean tryAwaitCommitUninterruptibly(Transaction tx, long timeout, TimeUnit unit) {
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
                statusCondition.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }

        if (isCommitted()) {
            tx.commit();
        } else {
            tx.abort();
        }

        throw new TodoException();
    }


//    /**
//     * Tries to
//     *
//     * This call is not interruptible.
//     *
//     * @param tx
//     * @return try if the VetoCommitBarrier was committed, false if aborted.
//     * @throws NullPointerException  if tx is null.
//     * @throws IllegalStateException if this VetoCommitBarrier already is aborted or committed.
//     */
//    public boolean tryAwaitCommit(Transaction tx){
//        if (tx == null) {
//            throw new NullPointerException();
//        }
//
//        if (!lock.tryLock()) {
//            abort();
//            if (tx.getStatus() != TransactionStatus.committed) {
//                tx.abort();
//            }
//            return false;
//        }
//
//        try {
//            switch (status) {
//                case open:
//                    tx.prepare();
//
//                    numberWaiting++;
//                    while (status == Status.open) {
//                        prepared.awaitUninterruptibly();
//                    }
//                    numberWaiting--;
//                    break;
//                case committed:
//                    String committedMsg = format("Can't await commit on already committed VetoCommitBarrier " +
//                            "with transaction %s", tx.getConfig().getFamilyName());
//                    throw new IllegalStateException(committedMsg);
//                case aborted:
//                    String abortMsg = format("Can't await commit on already aborted VetoCommitBarrier " +
//                            "with transaction %s", tx.getConfig().getFamilyName());
//                    throw new IllegalStateException(abortMsg);
//                default:
//                    throw new IllegalStateException();
//            }
//        } finally {
//            lock.unlock();
//        }
//
//        throw new TodoException();
//    }


}
