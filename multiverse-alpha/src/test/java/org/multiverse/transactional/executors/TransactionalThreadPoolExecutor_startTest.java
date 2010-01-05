package org.multiverse.transactional.executors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsStarted;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_startTest {

    @Test
    public void whenUnstarted_thenInitialization() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        executor.start();

        assertIsStarted(executor);
        assertEquals(1, executor.getCurrentPoolSize());
    }

    @Test
    public void whenStarted_callIgnored() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        executor.start();

        executor.start();
        assertIsStarted(executor);
    }

    @Test
    public void whenShutdown_thenIllegalStateException() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        executor.start();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(1000);
            }
        });

        sleepMs(100);
        executor.shutdown();

        try {
            executor.start();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(executor.isShutdown());
    }


    @Test
    public void whenTerminated_thenIllegalStateException() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();

        executor.shutdown();

        try {
            executor.start();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsTerminated(executor);
    }
}
