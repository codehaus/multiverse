package org.multiverse.transactional.nonblocking;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class NonBlockingTaskThreadPoolExecutor_integrationTest {
    private NonBlockingTaskThreadPoolExecutor executor;
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    @Ignore
    public void test() {
        executor = new NonBlockingTaskThreadPoolExecutor(1);

        NonBlockingTask task = new NonBlockingTask() {
            @Override
            public TransactionFactory getTransactionFactory() {
                return stm.getTransactionFactoryBuilder().build();
            }

            @Override
            public boolean execute(Transaction t) {
                System.out.println("hello");
                return false;
            }
        };
        executor.execute(task);
    }
}
