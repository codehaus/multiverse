package org.multiverse.transactional.executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestUtils;
import org.multiverse.api.Stm;

import java.util.concurrent.atomic.AtomicInteger;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalThreadPoolExecutor_exceptionsStressTest {

    private Stm stm;
    private TransactionalThreadPoolExecutor executor;
    private AtomicInteger todoCount;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();

        executor = new TransactionalThreadPoolExecutor();
        executor.setCorePoolSize(5);

        todoCount = new AtomicInteger(100000);
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTerminationUninterruptibly();
        }
    }

    @Test
    public void test() throws InterruptedException {
        for (int k = 0; k < executor.getCorePoolSize(); k++) {
            executor.execute(new Task());
        }

        executor.awaitTermination();
    }

    public class Task implements Runnable {

        @Override
        public void run() {
            int count = todoCount.decrementAndGet();
            if (count <= 0) {
                if (count == 0) {
                    executor.shutdown();
                }

                return;
            }

            executor.execute(new Task());

            if (TestUtils.randomOneOf(10)) {
                throw new RuntimeException();
            }
        }
    }
}
