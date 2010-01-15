package org.multiverse.transactional.executors;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorPerformanceLongTest {

    private int taskCount = 1000 * 1000;

    @Before
    public void setUp() {

    }

    @Test
    public void testThreadPoolExecutor() throws InterruptedException {
        test(new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue()));
    }

    @Test
    public void testTransactionalThreadPoolExecutor() throws InterruptedException {
        test(new TransactionalThreadPoolExecutor());
    }

    public void test(ExecutorService executor) throws InterruptedException {
        long startNs = System.nanoTime();

        for (int k = 0; k < taskCount; k++) {
            executor.execute(new Task());
        }

        executor.shutdown();
        if (executor instanceof TransactionalThreadPoolExecutor) {
            ((TransactionalThreadPoolExecutor) executor).awaitTermination();
        } else {
            executor.awaitTermination(1, TimeUnit.DAYS);
        }

        long elapsedNs = System.nanoTime() - startNs;
        System.out.printf("Execution of %s tasks took %s ms\n", taskCount, TimeUnit.NANOSECONDS.toMillis(elapsedNs));
    }

    public class Task implements Runnable {

        @Override
        public void run() {

        }
    }
}
