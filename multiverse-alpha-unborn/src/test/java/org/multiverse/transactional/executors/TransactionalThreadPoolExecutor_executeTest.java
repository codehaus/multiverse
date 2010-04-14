package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.concurrent.RejectedExecutionException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsStarted;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_executeTest {

    private TransactionalThreadPoolExecutor executor;
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdown();//TODO: should be shutdownNow but causes NPE, needs to be fixed
            executor.awaitTerminationUninterruptibly();
        }
    }

    @Test
    public void whenTaskIsNull_thenNullPointerException() {
        executor = new TransactionalThreadPoolExecutor();
        executor.start();

        try {
            executor.execute(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsStarted(executor);
    }

    @Test
    public void whenUnstarted_thenStartedAndTaskAccepted() {
        executor = new TransactionalThreadPoolExecutor();
        Runnable command = mock(Runnable.class);
        executor.execute(command);

        assertIsStarted(executor);
    }


    @Test
    public void whenStarted() {
        executor = new TransactionalThreadPoolExecutor();
        executor.start();

        Runnable task = mock(Runnable.class);
        executor.execute(task);

        sleepMs(200);

        assertIsStarted(executor);
        verify(task, times(1)).run();
    }

    @Test
    public void whenShutdown_thenRejectedExecutionException() {
        executor = new TransactionalThreadPoolExecutor();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                sleepMs(1000);
            }
        });

        executor.shutdown();

        Runnable task = mock(Runnable.class);

        try {
            executor.execute(task);
            fail();
        } catch (RejectedExecutionException expected) {
        }

        verify(task, never()).run();
    }

    @Test
    public void whenTerminated_thenRejectedExecutionException() {
        executor = new TransactionalThreadPoolExecutor();
        executor.shutdown();

        Runnable command = mock(Runnable.class);
        try {
            executor.execute(command);
            fail();
        } catch (RejectedExecutionException expected) {
        }

        assertIsTerminated(executor);
    }

}
