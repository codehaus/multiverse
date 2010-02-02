package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;

import static org.multiverse.TestUtils.*;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_awaitTerminationUninterruptiblyTest {

    private TransactionalThreadPoolExecutor executor;

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
    @Ignore
    public void whenStarted() {
    }

    @Test
    @Ignore
    public void whenShutdown() {
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
