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
public class ExponentialBackoffPolicy implements BackoffPolicy {

    public final static ExponentialBackoffPolicy INSTANCE_10_MS_MAX = new ExponentialBackoffPolicy();

    private final long maxDelayNs;
    private final long minDelayNs;

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
    public void delay(Transaction t, int attempt) throws InterruptedException {
        delayedUninterruptible(t, attempt);
    }


//    public final AtomicLong smallest = new AtomicLong(Long.MAX_VALUE);

    @Override
    public void delayedUninterruptible(Transaction t, int attempt) {
        long delayNs = calcDelayNs(attempt);

        long delay = System.nanoTime();

        if (delayNs > 1000) {
            parkNanos(delayNs);
        }

//        long found = System.nanoTime() - delay;
//        if (found < smallest.get()) {
//            smallest.set(found);
        //           System.out.println("smallest delay: " + found + "desired delay: " + delayNs);
//        }
    }

    //public final AtomicLong maxAttempt = new AtomicLong();
    //public final AtomicLong maxDelay = new AtomicLong();

    protected long calcDelayNs(int attempt) {

        //    if(attempt>maxAttempt.get()){
        //        maxAttempt.set(attempt);
        //        System.out.println("maxAttempt: " + attempt);
        //    }

        long delayNs;
        if (attempt <= 0) {
            delayNs = 0;
        } else if (attempt >= 63) {
            delayNs = Long.MAX_VALUE;
        } else {
            delayNs = 1L << attempt;
        }

        if (minDelayNs > 0 && delayNs < minDelayNs) {
            delayNs = minDelayNs;
        }

        if (maxDelayNs > 0 && delayNs > maxDelayNs) {
            delayNs = maxDelayNs;
        }

        //    if (maxDelay.get() < delayNs) {
        //        maxDelay.set(delayNs);
        //        System.out.println("delayNs: " + delayNs);
        //    }

        return delayNs;
    }
}

