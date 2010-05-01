package org.multiverse.integrationtests.notification;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.integrationtests.Ref;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A integration test that tests the wait notify mechanism.
 * <p/>
 * It uses a single IntValue a buch of threads listens to. Each thread listens a specific change. If this change
 * happens, it increases the number and waits for the next change. This means that threads keep increasing the
 * IntValue (in a rather complex manner). So if an event got lost, or more than 1 thread increases the counter
 * after receiving the event, the test would block (in case of a lost event) or the number of the intvalue after
 * completion would be larger than expected.
 *
 * @author Peter Veentjer.
 */
public class PingPongStressTest {
    private int pingPongCount = 1 * 1000 * 1000;
    private int threadCount = 2;
    private Ref intValue;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        intValue = new Ref(0);
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        PingPongThread[] threads = createThreads();
        startAll(threads);
        joinAll(threads);

        assertEquals(pingPongCount * threadCount, intValue.get());
    }

    private PingPongThread[] createThreads() {
        PingPongThread[] threads = new PingPongThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PingPongThread(k);
        }
        return threads;
    }

    private class PingPongThread extends TestThread {
        private int id;

        public PingPongThread(int id) {
            super("PingPongThread-" + id);
            this.id = id;
        }

        @Override
        public void doRun() {
            int expected = id;

            for (int k = 0; k < pingPongCount; k++) {
                if (k % (100 * 1000) == 0) {
                    System.out.println(getName() + " " + k);
                }

                intValue.await(expected);
                intValue.inc();
                expected += threadCount;
            }
        }
    }
}
