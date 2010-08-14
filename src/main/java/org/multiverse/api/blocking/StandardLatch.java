package org.multiverse.api.blocking;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

public final class StandardLatch implements Latch {
    private volatile boolean isOpen;
    private final Lock lock;
    private final Condition isOpenCondition;
    private volatile long era = Long.MIN_VALUE;

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
    public void prepareForPooling() {
        lock.tryLock();
        try {
            if (isOpen) {
                isOpen = false;
            } else {
                isOpenCondition.signalAll();
            }
            era++;            
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getEra() {
        return era;
    }

    @Override
    public void awaitUninterruptible(long expectedEra) {
        if (isOpen || expectedEra != era) {
            return;
        }

        lock.lock();
        try {
            while (!isOpen && expectedEra == era) {
                isOpenCondition.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void await(long expectedEra) throws InterruptedException {
        if (isOpen || expectedEra != era) {
            return;
        }

        lock.lock();
        try {
            while (!isOpen && expectedEra == era) {
                isOpenCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long tryAwait(long expectedEra, long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (isOpen || expectedEra != era) {
            return timeout;
        }

        long timeoutNs = unit.toNanos(timeout);
        return tryAwait(expectedEra, timeoutNs);
    }

    @Override
    public long tryAwaitUninterruptible(long expectedEra, long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (isOpen) {
            return timeout;
        }

        long timeoutNs = unit.toNanos(timeout);
        boolean restoreInterrupt = Thread.interrupted();
        try {
            do {
                long startNs = System.nanoTime();
                try {
                    return tryAwait(expectedEra, timeoutNs);
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

    private long tryAwait(long expectedEra, long timeoutNs) throws InterruptedException {
        if (isOpen || expectedEra != era) {
            return timeoutNs;
        }

        long startTimeNs = System.nanoTime();

        if (!lock.tryLock(timeoutNs, TimeUnit.NANOSECONDS)) {
            return -1;
        }

        timeoutNs -= System.nanoTime() - startTimeNs;
        try {
            while (!isOpen && expectedEra == era) {
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
    public void open(long expectedEra) {
        if (isOpen || expectedEra != era) {
            return;
        }

        lock.lock();
        try {
            if (isOpen || expectedEra != era) {
                return;
            }

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
        return format("StandardLatch(era=%s,open=%s)", era, isOpen);
    }
}
