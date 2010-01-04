package org.multiverse.transactional.executors;

import org.junit.Test;
import org.multiverse.transactional.collections.TransactionalLinkedList;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsUnstarted;

public class TransactionalThreadPoolExecutor_constructorTest {

    @Test
    public void emptyConstructor() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        assertIsUnstarted(executor);
        assertTrue(executor.getWorkQueue() instanceof TransactionalLinkedList);
    }

    @Test(expected = NullPointerException.class)
    public void whenWorkQueueNull_thenNullPointerException() {
        new TransactionalThreadPoolExecutor(null);
    }

    @Test
    public void whenWorkQueueConstructor() {
        TransactionalLinkedList<Runnable> workQueue = new TransactionalLinkedList<Runnable>();
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor(workQueue);
        assertIsUnstarted(executor);
        assertSame(workQueue, executor.getWorkQueue());
    }
}
