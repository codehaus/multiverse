package org.multiverse.commitbarriers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Peter Veentjer.
 */
class AbstractCommitBarrier {

    protected final Lock lock;
    protected final Condition statusCondition;

    protected volatile Status status = Status.open;
    protected volatile int numberWaiting = 0;

    public AbstractCommitBarrier(boolean fair) {
        lock = new ReentrantLock(fair);
        statusCondition = lock.newCondition();
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
     * Checks if this VetoCommitBarrier is open.
     *
     * @return true if open, false otherwise.
     */
    public final boolean isOpen() {
        return status == Status.open;
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
     * Signals to all prepared Transactions in this VetoCommitBarrier that they should abort.
     * <p/>
     * If the VetoCommitBarrier already is aborted, this call is ignored.
     *
     * @throws IllegalStateException if the VetoCommitBarrier already is committed.
     */
    public final void abort() {
        lock.lock();
        try {
            switch (status) {
                case open:
                    status = Status.aborted;
                    statusCondition.signalAll();
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
     * Awaits for this barrier to close (commit or abort).
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting.
     */
    public final void awaitClose() throws InterruptedException {
        if (status == Status.open) {
            lock.lockInterruptibly();
            try {
                while (status == Status.open) {
                    statusCondition.await();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Awaits for this barrier to close (commit or abort).
     * <p/>
     * This call is not responsive to interrupts.
     */
    public final void awaitCloseUninterruptibly() {
        if (status == Status.open) {
            lock.lock();
            try {
                while (status == Status.open) {
                    statusCondition.awaitUninterruptibly();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Waits for this barrier to close (abort or commit).
     *
     * @param timeout the maximum amount of time to wait for the barrier to close.
     * @param unit    the TimeUnit for the timeout argument.
     * @return true if the wait was a success, false if the barrier still is open.
     * @throws InterruptedException if the thread is interrupted while waiting.
     * @throws NullPointerException if unit is null.
     */
    public final boolean tryAwaitClose(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (status == Status.open) {
            long timeoutNs = unit.toNanos(timeout);

            lock.lockInterruptibly();
            try {
                while (status == Status.open) {
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
    public final boolean tryAwaitCloseUninterruptibly(long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (status == Status.open) {
            long timeoutNs = unit.toNanos(timeout);
            lock.lock();
            try {
                while (status == Status.open) {
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

    enum Status {
        open, committed, aborted
    }
}
