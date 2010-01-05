package org.multiverse.transactional.executors;

import org.junit.Test;
import org.multiverse.TestThread;

import static org.multiverse.TestUtils.*;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_awaitTerminationUninterruptiblyTest {

    @Test
    public void whenUnstarted() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        AwaitThread awaitThread = new AwaitThread(executor);
        awaitThread.start();

        sleepSome();
        assertAlive(awaitThread);

        executor.shutdown();
        sleepSome();
        assertNotAlive(awaitThread);
    }

    @Test
    public void whenStarted() {
        testIncomplete();
    }

    @Test
    public void whenShutdown() {
        testIncomplete();
    }

    @Test
    public void whenTerminated() throws InterruptedException {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        executor.shutdown();

        executor.awaitTerminationUninterruptibly();
        assertIsTerminated(executor);
    }

    private class AwaitThread extends TestThread {
        private final TransactionalThreadPoolExecutor executor;

        private AwaitThread(TransactionalThreadPoolExecutor executor) {
            super("AwaitThread");
            this.executor = executor;
        }

        @Override
        public void doRun() throws Exception {
            executor.awaitTerminationUninterruptibly();
        }
    }
}
