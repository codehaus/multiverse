package org.multiverse.stms.beta.benchmarks;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;

public class IntrinsicLockBenchmark {

    private volatile boolean stop = false;
    private final int threadCount = 2;
    private volatile long value;

    @Before
    public void setUp() {
        stop = false;
    }

    @Ignore
    @Test
    public void test() throws InterruptedException {
        PingPongThread[] threads = createThreads();

        startAll(threads);

        sleep(30 * 1000);
        stop = true;

        System.out.println("Waiting for joining threads");

        joinAll(threads);

        assertEquals(sum(threads), value);
    }

    private PingPongThread[] createThreads() {
        PingPongThread[] threads = new PingPongThread[threadCount];
        Object lock = new Object();
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PingPongThread(k, lock);
        }
        return threads;
    }

    private long sum(PingPongThread[] threads) {
        long result = 0;
        for (PingPongThread t : threads) {
            result += t.count;
        }
        return result;
    }

    private class PingPongThread extends TestThread {
        private final int id;
        private long count;
        private int expected;
        private final Object lock;

        public PingPongThread(int id, Object lock) {
            super("PingPongThread-" + id);
            this.id = id;
            this.lock = lock;
        }

        @Override
        public void doRun() {
            while (!stop) {
                if (count % (100 * 1000) == 0) {
                    System.out.println(getName() + " " + count);
                }

                synchronized (lock) {
                    while (value % threadCount != id) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ignore) {
                        }
                    }
                    value++;

                    lock.notifyAll();
                }

                expected += threadCount;
                count++;
            }
        }
    }
}
