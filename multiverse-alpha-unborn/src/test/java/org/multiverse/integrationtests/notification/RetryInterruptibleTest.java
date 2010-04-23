package org.multiverse.integrationtests.notification;

import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;

/**
 * A Test that checks if the retry can be interrupted if the transaction is configured as interruptible.
 *
 * @author Peter Veentjer.
 */
public class RetryInterruptibleTest {

    private TransactionalInteger ref;

    @Test
    public void test() throws InterruptedException {
        ref = new TransactionalInteger(0);

        AwaitThread t = new AwaitThread();
        t.start();

        sleepMs(200);
        assertAlive(t);
        t.interrupt();

        t.join();
        assertTrue(t.wasInterrupted);
    }

    class AwaitThread extends Thread {
        private boolean wasInterrupted;

        public void run() {
            try {
                await();
            } catch (InterruptedException e) {
                wasInterrupted = true;
            }
        }

        @TransactionalMethod(trackReads = true, interruptible = true)
        public void await() throws InterruptedException {
            ref.await(1);
        }
    }
}
