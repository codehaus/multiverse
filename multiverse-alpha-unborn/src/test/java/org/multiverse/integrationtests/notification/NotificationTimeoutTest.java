package org.multiverse.integrationtests.notification;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class NotificationTimeoutTest {

    private TransactionalInteger ref;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        ref = new TransactionalInteger();
    }

    @Test
    public void whenTimedOut() {
        try {
            await(1);
            fail();
        } catch (RetryTimeoutException expected) {
        }
    }

    @Test
    public void whenSomeWaitingNeeded() {
        SetThread t = new SetThread(1, 1);
        t.start();

        await(1);
    }

    @Test
    public void whenSpuriousWakeupsAndTimeout() {
        SetThread t = new SetThread(2, 1);
        t.start();

        try {
            await(1);
            fail();
        } catch (RetryTimeoutException expected) {
        }
    }

    @Test
    public void whenSpuriousWakeups() {
        SetThread t1 = new SetThread(2, 1);
        t1.start();

        SetThread t2 = new SetThread(1, 2);
        t2.start();

        await(1);
    }

    @Test
    public void whenNoRetryNeeded() {
        await(0);
    }

    @TransactionalMethod(timeout = 3)
    public void await(int value) {
        ref.await(value);
    }

    class SetThread extends TestThread {
        private final int value;
        private final int delayMs;

        SetThread(int value, int delayMs) {
            super("SetThread");
            this.value = value;
            this.delayMs = delayMs;
        }

        @Override
        public void doRun() throws Exception {
            sleepMs(delayMs);
            ref.set(value);
        }
    }

}
