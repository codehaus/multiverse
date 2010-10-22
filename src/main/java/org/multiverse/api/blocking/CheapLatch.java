package org.multiverse.api.blocking;

import org.multiverse.api.exceptions.TransactionInterruptedException;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * A Cheap {@link Latch} implementation based on the intrinsic lock.
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
    public void await(long expectedEra) {
        if (isOpen || expectedEra != era) {
            return;
        }

        try {
            synchronized (this) {
                while (!isOpen && era == expectedEra) {
                    wait();
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TransactionInterruptedException(exception);
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
        return tryAwaitUninterruptible(expectedEra, unit.toNanos(timeout));
    }

    @Override
    public long tryAwaitUninterruptible(long expectedEra, long timeoutNs) {
        if (isOpen || expectedEra != era) {
            return timeoutNs;
        }

        boolean restoreInterrupt = false;
        try {
            while (true) {
                long startNs = System.nanoTime();
                try {
                    synchronized (this) {
                        while (!isOpen && expectedEra == era) {
                            if (timeoutNs <= 0) {
                                return -1;
                            }

                            long ms = timeoutNs / 1000000;
                            int ns = (int) (timeoutNs % 1000000);
                            wait(ms, ns);
                            timeoutNs -= System.nanoTime() - startNs;
                        }

                        return timeoutNs;
                    }
                } catch (InterruptedException ex) {
                    restoreInterrupt = true;
                    timeoutNs -= System.nanoTime() - startNs;
                }
            }
        } finally {
            if (restoreInterrupt) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public long tryAwait(long expectedEra, long timeout, TimeUnit unit) {
        return tryAwait(expectedEra, unit.toNanos(timeout));
    }

    @Override
    public long tryAwait(long expectedEra, long timeoutNs) {
        if (isOpen || expectedEra != era) {
            return timeoutNs;
        }

        try {
            synchronized (this) {
                while (!isOpen && expectedEra == era) {
                    if (timeoutNs <= 0) {
                        return -1;
                    }

                    long ms = timeoutNs / 1000000;
                    int ns = (int) (timeoutNs % 1000000);
                    long startNs = System.nanoTime();
                    wait(ms, ns);
                    timeoutNs -= System.nanoTime() - startNs;
                }

                return timeoutNs;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TransactionInterruptedException(ex);
        }
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
