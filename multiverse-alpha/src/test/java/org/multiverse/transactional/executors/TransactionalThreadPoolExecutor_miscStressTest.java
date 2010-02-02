package org.multiverse.transactional.executors;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsUnstarted;

public class TransactionalThreadPoolExecutor_miscStressTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void longRunningTasksCanRunParallel() throws ExecutionException, InterruptedException {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        executor.setCorePoolSize(2);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                System.out.println("Start " + System.identityHashCode(Thread.currentThread()));
                sleepMs(5000);
                System.out.println("Finished " + System.identityHashCode(Thread.currentThread()));
            }
        };

        Future future1 = executor.submit(task);
        Future future2 = executor.submit(task);
        future1.get();
        future2.get();
        testIncomplete();
    }

    @Test
    public void executeIsAtomic() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        Runnable command = mock(Runnable.class);

        Transaction t = stm.getTransactionFactoryBuilder().setReadonly(false).build().start();
        setThreadLocalTransaction(t);

        executor.execute(command);
        executor.execute(command);
        t.abort();

        assertIsUnstarted(executor);
        testIncomplete();
    }
}
