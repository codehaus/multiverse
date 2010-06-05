package org.multiverse.integrationtests.notification;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.integrationtests.Ref;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
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
    private volatile boolean stop = false;
    private int threadCount = 2;
    private Ref ref;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        ref = new Ref(0);
        stop = false;
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        PingPongThread[] threads = createThreads();
        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);
        assertEquals(sum(threads), ref.get());
    }

    private PingPongThread[] createThreads() {
        PingPongThread[] threads = new PingPongThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PingPongThread(k);
        }
        return threads;
    }

    private long sum(PingPongThread[] threads){
        long result = 0;
        for(PingPongThread t: threads){
            result+=t.count;
        }
        return result;
    }

    private class PingPongThread extends TestThread {
        private int id;
        private int count;

        public PingPongThread(int id) {
            super("PingPongThread-" + id);
            this.id = id;
        }

        @Override
        public void doRun() {
            int expected = id;

            while(!stop){
                if (count % (100 * 1000) == 0) {
                    System.out.println(getName() + " " + count);
                }

                ref.await(expected);
                ref.inc();
                expected += threadCount;
                count++;
            }
        }
    }
}
