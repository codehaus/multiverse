package org.multiverse.utils.restartbackoff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ExponentialRestartBackoffPolicyTest {

    @Test
    public void construction_INSTANCE() {
        ExponentialRestartBackoffPolicy policy = ExponentialRestartBackoffPolicy.INSTANCE_10_MS_MAX;
        assertEquals(1000, policy.getMinDelayNs());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(10), policy.getMaxDelayNs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void minimumDelayCantBeLargerThanMaximumDelay() {
        new ExponentialRestartBackoffPolicy(10, 9, TimeUnit.NANOSECONDS);
    }

    @Test
    public void noDelayForZeroIteration() {
        ExponentialRestartBackoffPolicy policy = new ExponentialRestartBackoffPolicy();
        long delayNs = policy.calcDelayNs(0);
        assertEquals(policy.getMinDelayNs(), delayNs);
    }

    @Test
    public void minimumDelay() {
        long minDelay = 10000;
        ExponentialRestartBackoffPolicy policy = new ExponentialRestartBackoffPolicy(minDelay, 1, TimeUnit.SECONDS);

        long delayNs = policy.calcDelayNs(1);

        assertEquals(minDelay, delayNs);
    }

    @Test
    public void tooLargeValueTruncated() {
        long maxDelayNs = 10 * 1000;
        ExponentialRestartBackoffPolicy policy = new ExponentialRestartBackoffPolicy(
                100,
                maxDelayNs,
                TimeUnit.NANOSECONDS);

        long delayNs = policy.calcDelayNs(100);

        assertEquals(maxDelayNs, delayNs);
    }

    @Test
    public void happyFlow() {
        long maxDelayNs = 1000 * 1000;
        ExponentialRestartBackoffPolicy policy = new ExponentialRestartBackoffPolicy(
                100,
                maxDelayNs,
                TimeUnit.NANOSECONDS);

        assertEquals(1024, policy.calcDelayNs(10));
        assertEquals(4096, policy.calcDelayNs(12));
    }

    @Test
    public void sleep() throws InterruptedException {
        long sleepNs = TimeUnit.MILLISECONDS.toNanos(100);
        long startNs = System.nanoTime();
        ExponentialRestartBackoffPolicy.sleep(sleepNs);
        long elapsedNs = System.nanoTime() - startNs;

        //we need to build in some room for systems non perfect delays and time measurements. 
        assertTrue(elapsedNs > (0.95 * sleepNs));
    }
}
