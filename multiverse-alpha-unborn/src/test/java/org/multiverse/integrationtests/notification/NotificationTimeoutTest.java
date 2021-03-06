package org.multiverse.integrationtests.notification;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class NotificationTimeoutTest {

    private IntRef ref;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        ref = new IntRef();
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
        SetThread t = new SetThread(1, 1000);
        t.start();

        await(1);

        joinAll(t);
    }

    @Test
    public void whenSpuriousWakeupsAndTimeout() {
        SetThread t = new SetThread(2, 1000);
        t.start();

        try {
            await(1);
            fail();
        } catch (RetryTimeoutException expected) {
        }

        joinAll(t);
    }

    @Test
    public void whenSpuriousWakeups() {
        SetThread t1 = new SetThread(2, 1000);
        t1.start();

        SetThread t2 = new SetThread(1, 2000);
        t2.start();

        await(1);

        joinAll(t1, t2);
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
