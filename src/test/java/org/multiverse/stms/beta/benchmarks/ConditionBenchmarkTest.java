package org.multiverse.stms.beta.benchmarks;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;

public class ConditionBenchmarkTest {

    private volatile boolean stop = false;
    private final int threadCount = 2;
    private volatile long value;
    private ReentrantLock lock;
    private Condition condition;

    @Before
    public void setUp() {
        stop = false;
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    @Test
    @Ignore
    public void test() throws InterruptedException {
        PingPongThread[] threads = createThreads();

        for (PingPongThread thread : threads) {
            thread.start();
        }

        Thread.sleep(30 * 1000);
        stop = true;

        System.out.println("Waiting for joining threads");
        for (PingPongThread thread : threads) {
            thread.join();
        }

        assertEquals(sum(threads), value);

    }

    private PingPongThread[] createThreads() {
        PingPongThread[] threads = new PingPongThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PingPongThread(k);
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

    private class PingPongThread extends Thread {
        private final int id;
        private long count;
        private int expected;

        public PingPongThread(int id) {
            super("PingPongThread-" + id);
            this.id = id;
        }

        @Override
        public void run() {
            while (!stop) {
                if (count % (100 * 1000) == 0) {
                    System.out.println(getName() + " " + count);
                }

                lock.lock();
                try {
                    while (value % threadCount != id) {
                        try {
                            condition.await();
                        } catch (InterruptedException ignore) {
                        }
                    }
                    value++;

                    condition.signalAll();
                } finally {
                    lock.unlock();
                }

                expected += threadCount;
                count++;
            }
        }
    }
}
