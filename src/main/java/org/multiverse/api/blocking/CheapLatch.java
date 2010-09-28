package org.multiverse.api.blocking;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * A Cheap {@link Latch} implementation based on the intrinsic lock. This Latch has no support for bounded
 * waiting.
 *
 * @author Peter Veentjer
 */
public final class CheapLatch implements Latch {

    private volatile long era = Long.MIN_VALUE;
    private volatile boolean isOpen = false;

    @Override
    public void open(final long expectedEra) {
        if (isOpen || expectedEra != era) {
            return;
        }

        synchronized (this) {
            if (isOpen || expectedEra != era) {
                return;
            }

            isOpen = true;
            notifyAll();
        }
    }

    @Override
    public void await(long expectedEra) throws InterruptedException {
        if (isOpen || expectedEra != era) {
            return;
        }

        synchronized (this) {
            while (!isOpen && era == expectedEra) {
                wait();
            }
        }
    }

    @Override
    public void awaitUninterruptible(final long expectedEra) {
        if (isOpen || expectedEra != era) {
            return;
        }

        boolean restoreInterrupt = false;

        synchronized (this) {
            while (!isOpen && era == expectedEra) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    restoreInterrupt = true;
                }
            }
        }

        if (restoreInterrupt) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public long tryAwaitUninterruptible(long expectedEra, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long tryAwait(long expectedEra, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getEra() {
        return era;
    }

    @Override
    public void prepareForPooling() {
        synchronized (this) {
            if (!isOpen) {
                notifyAll();
            } else {
                isOpen = false;
            }
            era++;
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public String toString() {
        return format("CheapLatch(open=%s)", isOpen);
    }
}
