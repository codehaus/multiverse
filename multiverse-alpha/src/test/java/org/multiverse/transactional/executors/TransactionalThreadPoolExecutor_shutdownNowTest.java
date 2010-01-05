package org.multiverse.transactional.executors;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_shutdownNowTest {

    @Test
    public void whenUnstarted() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();

        List<Runnable> tasks = executor.shutdownNow();

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
        assertIsTerminated(executor);
    }

    @Test
    public void whenStarted() {
        testIncomplete();
    }

    @Test
    public void whenShutdown_thenRunningTaskInterrupted() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        executor.execute(new Runnable(){
            @Override
            public void run() {
                sleepMs(1000000000);
            }
        });

        //put the executor in the shutdown state.
        executor.shutdown();

        //now do a shutdownNow and see if the threadpool terminated
        executor.shutdownNow();

        sleepMs(300);
        assertTrue(executor.isTerminated());
    }

    @Test
    public void whenTerminated() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        executor.shutdown();

        List<Runnable> tasks = executor.shutdownNow();

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
        assertIsTerminated(executor);
    }
}
