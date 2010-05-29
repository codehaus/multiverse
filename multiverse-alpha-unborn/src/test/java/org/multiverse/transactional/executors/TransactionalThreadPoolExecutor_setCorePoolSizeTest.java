package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.getField;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.*;

public class TransactionalThreadPoolExecutor_setCorePoolSizeTest {
    private Stm stm;
    private TransactionalThreadPoolExecutor executor;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTerminationUninterruptibly();
        }
    }

    @Test
    public void whenUnstarted() {
        executor = new TransactionalThreadPoolExecutor();
        long version = stm.getVersion();

        executor.setCorePoolSize(10);

        assertEquals(version + 1, stm.getVersion());
        assertIsUnstarted(executor);
        assertEquals(10, executor.getCorePoolSize());
        assertEquals(0, executor.getCurrentPoolSize());
    }

    @Test
    public void whenValueSmallerThanZero_thenIllegalArgumentException() {
        executor = new TransactionalThreadPoolExecutor();

        long version = stm.getVersion();
        try {
            executor.setCorePoolSize(-1);
            fail();
        } catch (IllegalArgumentException ex) {
        }

        assertEquals(version, stm.getVersion());
        assertIsUnstarted(executor);
        assertEquals(1, executor.getCorePoolSize());
        assertEquals(0, executor.getCurrentPoolSize());
    }

    @Test
    public void whenNoChange() {
        executor = new TransactionalThreadPoolExecutor();
        executor.start();

        executor.setCorePoolSize(1);
        assertIsStarted(executor);
        assertEquals(1, executor.getCorePoolSize());
    }

    @Test
    public void whenPoolSizeIncreased() {
        executor = new TransactionalThreadPoolExecutor(1);
        executor.start();

         Runnable command = new Runnable() {
            @Override
            public void run() {
                sleepMs(2000);
            }
        };

        executor.execute(command);
        executor.execute(command);

        sleepMs(1000);
        assertEquals(1, executor.getWorkQueue().size());
        executor.setCorePoolSize(2);

        sleepMs(5000);
        assertEquals(0, executor.getWorkQueue().size());
    }

    @Test
    public void whenPoolSizeDecreasedAndWorkersDoingNothing() {
        executor = new TransactionalThreadPoolExecutor();
        executor.setCorePoolSize(5);
        executor.start();

        executor.setCorePoolSize(2);
        assertEquals(2, executor.getCorePoolSize());
    }

    @Test
    public void whenPoolSizeDecreasedAndWorkersWorking() {
        Runnable command = new Runnable() {
            @Override
            public void run() {
                sleepMs(1000);
            }
        };

        executor = new TransactionalThreadPoolExecutor();
        executor.setCorePoolSize(3);

        for (int k = 0; k < 3; k++) {
            executor.execute(command);
        }

        sleepMs(500);
        executor.setCorePoolSize(1);
        sleepMs(2000);

        List<Thread> threads = (List<Thread>) getField(executor, "threads");
        assertEquals(1, threads.size());
    }

    @Test
    public void whenShutdown_thenIllegalStateException() {
        executor = new TransactionalThreadPoolExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(3000);
            }
        });

        sleepMs(1500);
        executor.shutdown();

        try {
            executor.setCorePoolSize(10);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsShutdown(executor);
        assertEquals(1, executor.getCorePoolSize());
    }

    @Test
    public void whenTerminated_thenIllegalStateException() {
        executor = new TransactionalThreadPoolExecutor();
        executor.shutdown();

        try {
            executor.setCorePoolSize(10);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertEquals(1, executor.getCorePoolSize());
        assertIsTerminated(executor);
    }
}
