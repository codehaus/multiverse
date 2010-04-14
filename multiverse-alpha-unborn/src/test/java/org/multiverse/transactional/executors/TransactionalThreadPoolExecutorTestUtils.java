package org.multiverse.transactional.executors;

import static org.junit.Assert.assertEquals;

public class TransactionalThreadPoolExecutorTestUtils {

    public static void assertIsTerminated(TransactionalThreadPoolExecutor executor) {
        assertEquals(TransactionalThreadPoolExecutor.State.terminated, executor.getState());
    }

    public static void assertIsShutdown(TransactionalThreadPoolExecutor executor) {
        assertEquals(TransactionalThreadPoolExecutor.State.shutdown, executor.getState());
    }

    public static void assertIsUnstarted(TransactionalThreadPoolExecutor executor) {
        assertEquals(TransactionalThreadPoolExecutor.State.unstarted, executor.getState());
    }

    public static void assertIsStarted(TransactionalThreadPoolExecutor executor) {
        assertEquals(TransactionalThreadPoolExecutor.State.started, executor.getState());
    }
}
