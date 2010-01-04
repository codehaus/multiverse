package org.multiverse.transactional.executors;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsUnstarted;

public class TransactionalThreadPoolExecutor_setCorePoolSizeTest {
    private Stm stm;

    @Before
    public void setUp(){
         stm = getGlobalStmInstance();
    }

    @Test
    public void whenUnstarted() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        long version = stm.getVersion();

        executor.setCorePoolSize(10);

        assertEquals(version+1, stm.getVersion());
        assertIsUnstarted(executor);
        assertEquals(10, executor.getCorePoolSize());
        assertEquals(10, executor.getMaxPoolSize());
        assertEquals(0, executor.getCurrentPoolSize());
    }

    @Test
    public void whenUnstartedAndSmallerThanZero_thenIllegalArgumentException() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();

        long version = stm.getVersion();
        try {
            executor.setCorePoolSize(-1);
            fail();
        } catch (IllegalArgumentException ex) {
        }

        assertEquals(version, stm.getVersion());
        assertIsUnstarted(executor);
        assertEquals(1, executor.getCorePoolSize());
        assertEquals(1, executor.getMaxPoolSize());
        assertEquals(0, executor.getCurrentPoolSize());
    }

    @Test
    public void whenStarted() {
        testIncomplete();
    }

    @Test
    public void whenShutdown_thenIllegalStateException() {
        testIncomplete();
    }

    @Test
    public void whenTerminated_thenIllegalStateException() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
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
