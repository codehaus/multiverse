package org.multiverse.api.backoff;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ExponentialBackoffPolicyTest {

    @Test
    public void construction_INSTANCE() {
        ExponentialBackoffPolicy policy = ExponentialBackoffPolicy.INSTANCE_100_MS_MAX;
        assertEquals(100, policy.getMinDelayNs());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(100), policy.getMaxDelayNs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void minimumDelayCantBeLargerThanMaximumDelay() {
        new ExponentialBackoffPolicy(10, 9, TimeUnit.NANOSECONDS);
    }

    @Test
    @Ignore
    public void noDelayForZeroIteration() {
        ExponentialBackoffPolicy policy = new ExponentialBackoffPolicy();
        //long delayNs = policy.calcDelayNs(0);
        //assertEquals(policy.getMinDelayNs(), delayNs);
    }

    @Test
    @Ignore
    public void minimumDelay() {
        long minDelay = 10000;
        ExponentialBackoffPolicy policy = new ExponentialBackoffPolicy(minDelay, 1, TimeUnit.SECONDS);

        //long delayNs = policy.calcDelayNs(1);

        //assertEquals(minDelay, delayNs);
    }

    @Test
    @Ignore
    public void tooLargeValueTruncated() {
        long maxDelayNs = 10 * 1000;
        ExponentialBackoffPolicy policy = new ExponentialBackoffPolicy(
                100,
                maxDelayNs,
                TimeUnit.NANOSECONDS);

        //long delayNs = policy.calcDelayNs(100);

        //assertEquals(maxDelayNs, delayNs);
    }

    @Test
    @Ignore
    public void happyFlow() {
        long maxDelayNs = 1000 * 1000;
        ExponentialBackoffPolicy policy = new ExponentialBackoffPolicy(
                100,
                maxDelayNs,
                TimeUnit.NANOSECONDS);

        //assertEquals(1024, policy.calcDelayNs(10));
        //assertEquals(4096, policy.calcDelayNs(12));
    }
}
