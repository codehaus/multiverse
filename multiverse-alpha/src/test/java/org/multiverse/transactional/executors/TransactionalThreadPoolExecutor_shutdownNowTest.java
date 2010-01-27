package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_shutdownNowTest {


    private TransactionalThreadPoolExecutor executor;

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdown();
            executor.awaitTerminationUninterruptibly();
        }
    }


    @Test
    public void whenUnstarted() {
        executor = new TransactionalThreadPoolExecutor();

        List<Runnable> tasks = executor.shutdownNow();

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
        assertIsTerminated(executor);
    }

    @Test
    @Ignore
    public void whenStarted() {
    }

    @Test
    @Ignore
    public void whenShutdown_thenRunningTaskInterrupted() {
        executor = new TransactionalThreadPoolExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(1000);
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
        executor = new TransactionalThreadPoolExecutor();
        executor.shutdown();

        List<Runnable> tasks = executor.shutdownNow();

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
        assertIsTerminated(executor);
    }
}
