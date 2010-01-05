package org.multiverse.transactional.executors;

import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_awaitTerminationTest {

    @Test
    public void whenUnstarted() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();

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
