package org.multiverse.api.backoff;

import org.multiverse.api.Transaction;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * A {@link BackoffPolicy} that does an exponential backoff. So each next attempt, the delay is doubled until a
 * configurable maximum delay has been reached.
 * <p/>
 * The exponential growth of delay can be truncated by providing a maxDelay. If no max delay is provided, the maximum
 * delay would be 10.000.000.000 seconds (292 years). So be careful with not using an acceptable maximum delay.
 *
 * @author Peter Veentjer.
 */
public final class ExponentialBackoffPolicy implements BackoffPolicy {

    public final static ExponentialBackoffPolicy INSTANCE_10_MS_MAX = new ExponentialBackoffPolicy();

    private final long maxDelayNs;
    private final long minDelayNs;
    private final long[] slotTimes;

    /**
     * Creates an ExponentialBackoffPolicy with 100 nanoseconds as minimal delay and 10 milliseconds as maximum
     * delay.
     */
    public ExponentialBackoffPolicy() {
        this(10, MILLISECONDS.toNanos(10), TimeUnit.NANOSECONDS);

    }

    /**
     * Creates an ExponentialBackoffPolicy with given maximum delay.
     *
     * @param minDelayNs the minimum delay in nanoseconds to wait. If a negative or zero value provided, it will be
     *                   interpreted that no external minimal value is needed.
     * @param maxDelay   the maximum delay.
     * @param unit       the unit of maxDelay.
     * @throws NullPointerException if unit is null.
     */
    public ExponentialBackoffPolicy(long minDelayNs, long maxDelay, TimeUnit unit) {
        this.maxDelayNs = unit.toNanos(maxDelay);
        this.minDelayNs = minDelayNs;
        if (minDelayNs > maxDelayNs) {
            throw new IllegalArgumentException("minimum delay can't be larger than maximum delay");
        }

        slotTimes = new long[1000];
        for (int k = 0; k < slotTimes.length; k++) {
            slotTimes[k] = Math.round((1.0d * k) / slotTimes.length * maxDelayNs);
        }
    }

    /**
     * Returns the maximum delay in nanoseconds. A negative or zero delay indicates that there is no max.
     *
     * @return the maximum delay in nanosecond.
     */
    public long getMaxDelayNs() {
        return maxDelayNs;
    }

    /**
     * Returns the minimum delay in nanoseconds. A negative or zero value indicates that there is no minimal delay.
     *
     * @return the minimum delay in nanoseconds.
     */
    public long getMinDelayNs() {
        return minDelayNs;
    }

    @Override
    public void delay(Transaction t) throws InterruptedException {
        delayedUninterruptible(t);
    }

    @Override
    public void delayedUninterruptible(Transaction t) {
        long delayNs = calcDelayNs(t);

        if (delayNs > 1000) {
            parkNanos(delayNs);
        }
    }

    protected long calcDelayNs(Transaction tx) {
        if (tx.getAttempt() > 100) {
            System.out.println("tx.attempt: " + tx.getAttempt());
        }

        int maxSlot;
        int attempt = tx.getAttempt();
        if (attempt >= slotTimes.length) {
            maxSlot = slotTimes.length - 1;
        } else {
            maxSlot = attempt;
        }

        if (maxSlot <= 0) {
            maxSlot = 1;
        }

        int slotIndex = (int) Math.abs(System.identityHashCode(Thread.currentThread()) % maxSlot);
        //System.out.println("slotIndex: "+slotIndex);
        long delayNs = slotTimes[slotIndex];

        if (minDelayNs > 0 && delayNs < minDelayNs) {
            delayNs = minDelayNs;
        }

        if (maxDelayNs > 0 && delayNs > maxDelayNs) {
            delayNs = maxDelayNs;
        }
        return delayNs;
    }
}

