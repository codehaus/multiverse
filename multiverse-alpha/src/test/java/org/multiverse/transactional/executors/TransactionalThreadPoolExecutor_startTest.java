package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsStarted;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_startTest {


    private TransactionalThreadPoolExecutor executor;

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdown();
            executor.awaitTerminationUninterruptibly();
        }
    }


    @Test
    public void whenUnstarted_thenInitialization() {
        executor = new TransactionalThreadPoolExecutor();
        executor.start();

        assertIsStarted(executor);
        assertEquals(1, executor.getCurrentPoolSize());
    }

    @Test
    public void whenStarted_callIgnored() {
        executor = new TransactionalThreadPoolExecutor();
        executor.start();

        executor.start();
        assertIsStarted(executor);
    }

    @Test
    public void whenShutdown_thenIllegalStateException() {
        executor = new TransactionalThreadPoolExecutor();
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
        executor = new TransactionalThreadPoolExecutor();

        executor.shutdown();

        try {
            executor.start();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsTerminated(executor);
    }
}
