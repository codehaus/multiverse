package org.multiverse.utils.restartbackoff;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.multiverse.api.Transaction;

import java.util.concurrent.TimeUnit;

/**
 * @author Peter Veentjer
 */
public class NoOpRestartBackoffPolicyTest {

    @Test
    public void delay() throws InterruptedException {
        NoOpRestartBackoffPolicy policy = new NoOpRestartBackoffPolicy();
        long startNs = System.nanoTime();
        policy.delay(mock(Transaction.class), 0);
        policy.delay(mock(Transaction.class), 100);
        long elapsedNs = System.nanoTime() - startNs;
        assertTrue(elapsedNs < TimeUnit.MILLISECONDS.toNanos(1));
    }

    @Test
    public void delayUninterruptible() throws InterruptedException {
        NoOpRestartBackoffPolicy policy = new NoOpRestartBackoffPolicy();
        long startNs = System.nanoTime();
        policy.backoffUninterruptible(mock(Transaction.class), 0);
        policy.backoffUninterruptible(mock(Transaction.class), 100);
        long elapsedNs = System.nanoTime() - startNs;
        assertTrue(elapsedNs < TimeUnit.MILLISECONDS.toNanos(1));
    }
}
