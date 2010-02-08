package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_shutdownTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        executor = new TransactionalThreadPoolExecutor();
        clearThreadLocalTransaction();
    }


    private TransactionalThreadPoolExecutor executor;

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTerminationUninterruptibly();
        }
    }

    @Test
    public void whenUnstarted_thenTerminated() {
        executor.shutdown();
        assertIsTerminated(executor);
    }

    @Test
    public void whenStartedButNoTaskRunning_thenTerminate() {
        executor.start();

        executor.shutdown();
        sleepMs(100);
        assertTrue(executor.isTerminated());
    }

    @Test
    public void whenStartedAndTaskRunning_thenShutdown() {
        executor.start();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(1000);
            }
        });

        executor.shutdown();
        sleepMs(100);
        assertTrue(executor.isShutdown());
    }

    @Test
    public void whenShutdown_thenCallIgnored() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(1000);
            }
        });

        //put it in the shutdown state
        executor.shutdown();

        //new call the shutdown
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }

    @Test
    public void whenTerminated_thenCallIgnored() {
        executor.shutdown();

        executor.shutdown();

        long version = stm.getVersion();
        executor.shutdown();
        assertEquals(version, stm.getVersion());
        assertIsTerminated(executor);
    }
}
