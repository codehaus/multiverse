package org.multiverse.api;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * A {@link ExponentialBackoffPolicy} that does an exponential backoff. So each next attempt, the delay is doubled until a
 * configurable maximum delay has been reached.
 * <p/>
 * The exponential growth of delay can be truncated by providing a maxDelay. If no max delay is provided, the maximum
 * delay would be 10.000.000.000 seconds (292 years). So be careful with not using an acceptable maximum delay.
 *
 * @author Peter Veentjer.
 */
public final class ExponentialBackoffPolicy implements BackoffPolicy {

    public final static BackoffPolicy MAX_100_MS = new ExponentialBackoffPolicy();

    private final long maxDelayNs;
    private final long minDelayNs;
    private final long[] slotTimes;

    /**
     * Creates an ExponentialBackoffPolicy with 100 nanoseconds as minimal delay and 100 milliseconds as maximum
     * delay.
     */
    public ExponentialBackoffPolicy() {
        this(1000, TimeUnit.MILLISECONDS.toNanos(100), TimeUnit.NANOSECONDS);
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

        Random random = new Random();
        slotTimes = new long[1000];

        double a = 100;
        double b = -4963;
        for (int k = 0; k < slotTimes.length; k++) {
            slotTimes[k] = Math.round(random.nextDouble() * f(k, a, b));
        }
    }

    private int f(int x, double a, double b) {
        int result = (int) Math.round(a * x * x + b * x);
        return result < 0 ? 0 : result;
    }

    @Override
    public void delay(int attempt) throws InterruptedException {
        delayedUninterruptible(attempt);
    }

    @Override
    public void delayedUninterruptible(int attempt) {
        long delayNs = calcDelayNs(attempt);

        if (delayNs >= minDelayNs) {
            parkNanos(delayNs);
        } else if (attempt % 20 == 0) {
            Thread.yield();
        }
    }

    protected long calcDelayNs(int attempt) {
        int slotIndex = attempt >= slotTimes.length ? slotTimes.length - 1 : attempt;
        return slotTimes[slotIndex];
    }
}

