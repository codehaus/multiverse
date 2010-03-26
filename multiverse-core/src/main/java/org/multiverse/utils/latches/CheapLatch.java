package org.multiverse.utils.latches;

import org.multiverse.api.Latch;

import java.util.concurrent.TimeUnit;

/**
 * A {@link Latch} based on the intrinsic lock and uses the minimal amount of resources. It uses the CheapLatch object
 * itself for the monitor lock. It can't do any timed waits.
 * <p/>
 * If bounded waits are needed, a new latch needs to be added. A simple implementation would be one
 * based on {@link java.util.concurrent.locks.Lock} because it supports timed waits.
 *
 * @author Peter Veentjer.
 */
@SuppressWarnings({"SynchronizeOnThis", "WaitOrAwaitWithoutTimeout"})
public final class CheapLatch implements Latch {

    public final static CheapLatch OPEN_LATCH = new CheapLatch(true);

    private volatile boolean isOpen;

    /**
     * Creates a new closed CheapLatch.
     */
    public CheapLatch() {
        this(false);
    }

    /**
     * Creates a new CheapLatch.
     *
     * @param isOpen true if the latch already is closed, false if the latch is closed.
     */
    public CheapLatch(boolean isOpen) {
        this.isOpen = isOpen;
    }

    @Override
    public void await() throws InterruptedException {
        if (isOpen) {
            return;
        }

        synchronized (this) {
            while (!isOpen) {
                wait();
            }
        }
    }

    @Override
    public void awaitUninterruptible() {
        if (isOpen) {
            return;
        }

        boolean restoreInterrupt = false;

        try {
            synchronized (this) {
                while (!isOpen) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        restoreInterrupt = true;
                    }
                }
            }
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

        synchronized (this) {
            isOpen = true;
            notifyAll();
        }
    }

    /**
     * This operation is not supported on the CheapLatch.
     *
     * @throws UnsupportedOperationException because timed waits are not supported by this CheapLatch.
     */
    @Override
    public boolean tryAwait(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported on the CheapLatch.
     *
     * @throws UnsupportedOperationException because timed waits are not supported by this CheapLatch.
     */
    @Override
    public boolean tryAwaitUninterruptible(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public String toString() {
        return isOpen() ? "CheapLatch(open=true)" : "CheapLatch(open=false)";
    }
}
