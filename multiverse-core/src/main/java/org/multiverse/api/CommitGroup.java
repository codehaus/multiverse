package org.multiverse.api;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Peter Veentjer.
 */
public class CommitGroup {

    private volatile Status status = Status.open;

    private final Lock lock = new ReentrantLock();
    private final Condition readyToCommit = lock.newCondition();

    /**
     * Checks if this CommitGroup already is committed.
     *
     * @return true if committed, false otherwise.
     */
    public boolean isCommitted() {
        return status == Status.closedWithCommit;
    }

    /**
     * Checks if this CommitGroup already is aborted.
     *
     * @return true if aborted, false otherwise.
     */
    public boolean isAborted() {
        return status == Status.closedWithAbort;
    }

    public void signalCommit() {
        signal(true);
    }

    public void signalAbort() {
        signal(false);
    }

    private void signal(boolean commit) {
        lock.lock();
        try {
            if (status != Status.open) {
                throw new IllegalStateException();
            }

            status = commit ? Status.closedWithCommit : Status.closedWithAbort;
            readyToCommit.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void join(Transaction tx) throws InterruptedException {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.getStatus() != TransactionStatus.active) {
            throw new IllegalStateException();
        }

        lock.lock();
        try {
            if (status != Status.open) {
                throw new IllegalStateException();
            }

            tx.prepare();

            while (status == Status.open) {
                readyToCommit.await();
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

    enum Status {

        open, closedWithCommit, closedWithAbort
    }
}
