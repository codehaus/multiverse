package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_awaitTerminationUninterruptiblyTest {

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
    public void whenUnstarted() {
        executor = new TransactionalThreadPoolExecutor();
        AwaitThread awaitThread = new AwaitThread();
        awaitThread.start();

        sleepSome();
        assertAlive(awaitThread);

        executor.shutdown();
        sleepSome();
        assertNotAlive(awaitThread);
    }

    @Test
    public void whenStarted() throws InterruptedException {
        executor = new TransactionalThreadPoolExecutor();
        executor.start();

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                executor.awaitTermination();
            }
        };
        t.start();
        sleepSome();

        assertAlive(t);

        executor.shutdownNow();

        t.join();
        sleepSome();
        t.assertNothingThrown();
    }

    @Test
    public void whenShutdown() throws InterruptedException {
        executor = new TransactionalThreadPoolExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(1000000);
            }
        });

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                executor.awaitTermination();
            }
        };
        t.start();
        sleepSome();

        assertAlive(t);

        executor.shutdownNow();
        t.join();
        t.assertNothingThrown();
        assertIsTerminated(executor);
    }

    @Test
    public void whenTerminated() throws InterruptedException {
        executor = new TransactionalThreadPoolExecutor();
        executor.shutdown();

        executor.awaitTerminationUninterruptibly();
        assertIsTerminated(executor);
    }

    private class AwaitThread extends TestThread {
        private AwaitThread() {
            super("AwaitThread");
        }

        @Override
        public void doRun() throws Exception {
            executor.awaitTerminationUninterruptibly();
        }
    }
}
