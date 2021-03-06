package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_awaitTerminationLongTest {
    private TransactionalThreadPoolExecutor executor;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTerminationUninterruptibly();
        }
    }

    @Test
    public void whenUnstarted() throws InterruptedException {
        executor = new TransactionalThreadPoolExecutor();

        TestThread thread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                executor.awaitTermination();
            }
        };
        thread.start();
        sleepMs(1000);

        executor.shutdownNow();
        thread.join();
        thread.assertNothingThrown();

        assertTrue(executor.isTerminated());
    }

    @Test
    public void whenStarted() throws InterruptedException {
        executor = new TransactionalThreadPoolExecutor();
        executor.start();

        TestThread thread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                executor.awaitTermination();
            }
        };
        thread.start();
        sleepMs(1000);

        executor.shutdownNow();
        thread.join();
        thread.assertNothingThrown();

        assertTrue(executor.isShutdown() || executor.isTerminated());
    }

    @Test
    public void whenShutdown() throws InterruptedException {
        executor = new TransactionalThreadPoolExecutor();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                sleepMs(50000);
            }
        };

        executor.execute(task);
        sleepMs(1000);
        executor.shutdown();

        TestThread thread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                executor.awaitTermination();
            }
        };
        thread.start();
        sleepMs(1000);

        assertAlive(thread);

        executor.shutdownNow();
        thread.join();
        thread.assertNothingThrown();
    }

    @Test
    public void whenTerminated() throws InterruptedException {
        executor = new TransactionalThreadPoolExecutor();
        executor.shutdown();

        boolean result = executor.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(result);
        assertIsTerminated(executor);
    }
}
