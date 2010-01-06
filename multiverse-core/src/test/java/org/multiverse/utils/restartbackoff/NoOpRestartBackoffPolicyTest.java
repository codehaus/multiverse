package org.multiverse.utils.restartbackoff;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.multiverse.DummyTransaction;

import java.util.concurrent.TimeUnit;

/**
 * @author Peter Veentjer
 */
public class NoOpRestartBackoffPolicyTest {

    @Test
    public void delay() throws InterruptedException {
        NoOpRestartBackoffPolicy policy = new NoOpRestartBackoffPolicy();
        long startNs = System.nanoTime();
        policy.delay(new DummyTransaction(), 0);
        policy.delay(new DummyTransaction(), 100);
        long elapsedNs = System.nanoTime() - startNs;
        assertTrue(elapsedNs < TimeUnit.MILLISECONDS.toNanos(1));
    }

    @Test
    public void delayUninterruptible() throws InterruptedException {
        NoOpRestartBackoffPolicy policy = new NoOpRestartBackoffPolicy();
        long startNs = System.nanoTime();
        policy.delayUninterruptible(new DummyTransaction(), 0);
        policy.delayUninterruptible(new DummyTransaction(), 100);
        long elapsedNs = System.nanoTime() - startNs;
        assertTrue(elapsedNs < TimeUnit.MILLISECONDS.toNanos(1));
    }
}
