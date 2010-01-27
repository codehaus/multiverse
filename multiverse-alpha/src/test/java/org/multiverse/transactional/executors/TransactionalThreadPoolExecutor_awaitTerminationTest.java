package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_awaitTerminationTest {
    private TransactionalThreadPoolExecutor executor;

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdown();
            executor.awaitTerminationUninterruptibly();
        }
    }

    @Test
    @Ignore
    public void whenUnstarted() {

    }

    @Test
    @Ignore
    public void whenStarted_thenUnsupportedOperationException() {

    }

    @Test
    @Ignore
    public void whenShutdown() {
    }

    @Test
    public void whenTerminated() throws InterruptedException {
        executor = new TransactionalThreadPoolExecutor();
        executor.shutdown();

        boolean result = executor.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(result);
        assertIsTerminated(executor);
    }

    public class AwaitThread extends TestThread {
        private final ThreadPoolExecutor threadPoolExecutor;

        public AwaitThread(ThreadPoolExecutor threadPoolExecutor) {
            super("AwaitThread");
            this.threadPoolExecutor = threadPoolExecutor;
        }

        @Override
        public void doRun() throws Exception {
            //threadPoolExecutor.aw            
        }
    }

}
