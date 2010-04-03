package org.multiverse.api.latches;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A standard Latch implementation that supports all features of the Latch. Its implementation
 * is based on the {@link Lock} and {@link Condition}.
 *
 * @author Peter Veentjer
 */
public final class StandardLatch implements Latch {
    private volatile boolean isOpen;
    private final Lock lock;
    private final Condition isOpenCondition;

    /**
     * Creates a new closed StandardLatch.
     */
    public StandardLatch() {
        this(false);
    }

    /**
     * Creates a new StandardLatch.
     *
     * @param isOpen true if the latch already is closed, false otherwise.
     */
    public StandardLatch(boolean isOpen) {
        this.isOpen = isOpen;
        this.lock = new ReentrantLock();
        this.isOpenCondition = lock.newCondition();
    }

    @Override
    public void await() throws InterruptedException {
        if (isOpen) {
            return;
        }

        lock.lockInterruptibly();
        try {
            while (!isOpen) {
                isOpenCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void awaitUninterruptible() {
        if (isOpen) {
            return;
        }

        lock.lock();
        try {
            while (!isOpen) {
                isOpenCondition.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean tryAwait(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (isOpen) {
            return true;
        }

        long remainingNs = tryAwait(unit.toNanos(timeout));
        return remainingNs > 0;
    }

    private long tryAwait(long timeoutNs) throws InterruptedException {
        long startTimeNs = System.nanoTime();

        if (!lock.tryLock(timeoutNs, TimeUnit.NANOSECONDS)) {
            return -1;
        }

        timeoutNs -= System.nanoTime() - startTimeNs;
        try {
            while (!isOpen) {
                timeoutNs = isOpenCondition.awaitNanos(timeoutNs);

                if (timeoutNs <= 0) {
                    return -1;
                }
            }

            return timeoutNs;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean tryAwaitUninterruptible(long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (isOpen) {
            return true;
        }

        long timeoutNs = unit.toNanos(timeout);

        boolean restoreInterrupt = Thread.interrupted();
        try {
            do {
                long startNs = System.nanoTime();
                try {
                    return tryAwait(timeoutNs) > 0;
                } catch (InterruptedException ex) {
                    restoreInterrupt = true;
                    timeoutNs -= System.nanoTime() - startNs;
                }
            } while (true);
        } finally {
            if (restoreInterrupt) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void open() {
        if (isOpen) {
            return;
        }

        lock.lock();
        try {
            isOpen = true;
            isOpenCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public String toString() {
        return isOpen ? "StandardLatch(open=true)" : "StandardLatch(open=false)";
    }
}
