package org.multiverse.transactional.executors;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsTerminated;

public class TransactionalThreadPoolExecutor_setMaxPoolSizeTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenUnstarted() {
        testIncomplete();
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

        long version = stm.getVersion();

        try {
            executor.setMaxPoolSize(10);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertEquals(1, executor.getMaxPoolSize());
        assertIsTerminated(executor);
        assertEquals(version, stm.getVersion());
    }
}
