package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_shutdownNowTest {

    private TransactionalThreadPoolExecutor executor;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTerminationUninterruptibly();
        }
        clearThreadLocalTransaction();
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
    public void whenStarted() {
        executor = new TransactionalThreadPoolExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(100000);
            }
        });

        sleepMs(500);

        //do a shutdownNow and see if the threadpool terminated
        List<Runnable> sink = executor.shutdownNow();

        assertTrue(sink.isEmpty());
        sleepMs(500);
        assertTrue(executor.isTerminated());
    }

    @Test
    public void whenStartedAndPendingWork_thenPendingWorkReturned() {
        executor = new TransactionalThreadPoolExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(100000);
            }
        });
        Runnable command = mock(Runnable.class);
        executor.execute(command);
        sleepMs(500);
        //do a shutdownNow and see if the threadpool terminated
        List<Runnable> sink = executor.shutdownNow();

        assertEquals(asList(command), sink);
        sleepMs(500);
        assertTrue(executor.isTerminated());
    }

    @Test
    public void whenShutdown_thenRunningTaskInterrupted() {
        executor = new TransactionalThreadPoolExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(100000);
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
    @Ignore
    public void whenShutdownAndPendingWork_thenPendingReturned() {
        executor = new TransactionalThreadPoolExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(100000);
            }
        });
        Runnable command = new DummyTask();
        executor.execute(command);
        sleepMs(500);
        executor.shutdown();
        sleepMs(500);

        //now do a shutdownNow and see if the threadpool terminated
        List<Runnable> sink = executor.shutdownNow();

        assertEquals(asList(command), sink);
        sleepMs(500);
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

    class DummyTask implements Runnable {
        @Override
        public void run() {
        }
    }
}
