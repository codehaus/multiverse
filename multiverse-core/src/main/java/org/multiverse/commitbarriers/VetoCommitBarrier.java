package org.multiverse.commitbarriers;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.TodoException;

import java.util.List;
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
        super(Status.closed, fair);
    }

    /**
     * Signals to all prepared Transactions in this VetoCommitBarrier that they should commit.
     * <p/>
     * If the VetoCommitBarrier already is committed, this call is ignored.
     *
     * @throws ClosedCommitBarrierException if the VetoCommitBarrier already is aborted.
     */
    public void commit() {
        List<Runnable> postCommitTasks = null;
        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    postCommitTasks = signalCommit();
                    break;
                case committed:
                    //ignore it.
                    return;
                case aborted:
                    throw new ClosedCommitBarrierException("Can't abort already committed VetoCommitBarrier");
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        executeTasks(postCommitTasks);
    }

    /**
     * Commits this Transaction and all other transactions in the VetoCommitBarrier that have prepared.
     * <p/>
     * If the VetoCommitBarrier already is aborted or committed, the transaction is aborted.
     *
     * @param tx the Transaction to commit.
     * @throws NullPointerException     if tx is null.
     * @throws DeadTransactionException if the Transaction already is aborted or committed.
     * @throws org.multiverse.api.exceptions.CommitFailureException
     *                                  if the commit was not executed successfully.
     * @throws IllegalStateException    if the VetoCommitBarrier isn't closed anymore.
     */
    public void commit(Transaction tx) {
        ensureNotDead(tx);

        List<Runnable> postCommitTasks = null;
        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();
                    postCommitTasks = signalCommit();
                    break;
                case aborted:
                    String abortedMsg = format("Can't call commit on already aborted VetoCommitBarrier");
                    throw new ClosedCommitBarrierException(abortedMsg);
                case committed:
                    String committedMsg = format("Can't call commit on already committed VetoCommitBarrier");
                    throw new ClosedCommitBarrierException(committedMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        tx.commit();
        executeTasks(postCommitTasks);
    }

    /**
     * Awaits for the tx to commit. It will commit when all transactions on the group are going to commit.
     * <p/>
     * If the VetoCommitBarrier already is aborted or committed, the transaction is aborted.
     *
     * @param tx the Transaction to commit.
     * @throws InterruptedException         if the thread is interrupted while waiting.
     * @throws NullPointerException         if tx is null.
     * @throws DeadTransactionException     if tx is committed/aborted.
     * @throws ClosedCommitBarrierException if this VetoCommitBarrier is committed or aborted.
     */
    public void awaitCommit(Transaction tx) throws InterruptedException {
        ensureNotDead(tx);

        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();

                    addWaiter(tx);
                    while (getStatus() == Status.closed) {
                        statusCondition.await();
                    }
                    break;
                case committed:
                    String committedMsg = format("Can't await commit on already committed VetoCommitBarrier " +
                            "with transaction %s", tx.getConfig().getFamilyName());
                    throw new ClosedCommitBarrierException(committedMsg);
                case aborted:
                    String abortMsg = format("Can't await commit on already aborted VetoCommitBarrier " +
                            "with transaction %s", tx.getConfig().getFamilyName());
                    throw new ClosedCommitBarrierException(abortMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        //todo: the the thread is interrupted, tx is not aborted.
        finish(tx);
    }

    /**
     * This call is not responsive to interrupts.
     * <p/>
     * If this VetoCommitBarrier already is aborted or committed, the transaction is aborted.
     *
     * @throws NullPointerException         if transaction is null.
     * @throws ClosedCommitBarrierException if this VetoCommitBarrier already is aborted or committed.
     */
    public void awaitCommitUninterruptible(Transaction tx) {
        ensureNotDead(tx);

        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();

                    addWaiter(tx);
                    while (getStatus() == Status.closed) {
                        statusCondition.awaitUninterruptibly();
                    }
                    break;
                case committed:
                    String commitMsg = "Can't await commit on already committed VetoCommitBarrier";
                    throw new ClosedCommitBarrierException(commitMsg);
                case aborted:
                    String abortMsg = "Can't await commit on already aborted VetoCommitBarrier";
                    throw new ClosedCommitBarrierException(abortMsg);
                default:
                    throw new IllegalStateException();
            }

        } finally {
            lock.unlock();
        }

        finish(tx);
    }

    public boolean tryAwaitCommit(Transaction tx, long timeout, TimeUnit unit) throws InterruptedException {
        ensureNotDead(tx);

        if (unit == null) {
            throw new NullPointerException();
        }

        throw new TodoException();
    }

    public boolean tryAwaitCommitUninterruptibly(Transaction tx, long timeout, TimeUnit unit) {
        ensureNotDead(tx);

        long timeoutNs = unit.toNanos(timeout);
        if (timeoutNs < 0) {
            //todo: aborten?
            return false;
        }

        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    tx.prepare();
                    addWaiter(tx);
                    while (getStatus() == Status.closed) {
                        statusCondition.awaitUninterruptibly();
                    }
                    break;
                case committed:
                    String commitMsg = "Can't await commit on an already committed VetoCommitBarrier";
                    throw new ClosedCommitBarrierException(commitMsg);
                case aborted:
                    String abortMsg = "Can't await commit on an already aborted VetoCommitBarrier";
                    throw new ClosedCommitBarrierException(abortMsg);
                default:
                    throw new NullPointerException();
            }
        } finally {
            lock.unlock();
        }

        finish(tx);

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
//                case closed:
//                    tx.prepare();
//
//                    numberWaiting++;
//                    while (status == Status.closed) {
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
