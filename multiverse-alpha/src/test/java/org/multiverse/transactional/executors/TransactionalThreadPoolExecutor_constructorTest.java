package org.multiverse.transactional.executors;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.collections.TransactionalLinkedList;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.executors.TransactionalThreadPoolExecutorTestUtils.assertIsUnstarted;

public class TransactionalThreadPoolExecutor_constructorTest {

    @Before
    public void setUp(){
           clearThreadLocalTransaction();
    }

    @Test
    public void withEmptyConstructor() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor();
        assertIsUnstarted(executor);
        assertTrue(executor.getWorkQueue() instanceof TransactionalLinkedList);
    }

    @Test(expected = NullPointerException.class)
    public void withWorkQueue_whenWorkQueueNull_thenNullPointerException() {
        new TransactionalThreadPoolExecutor(null);
    }

    @Test
    public void withWorkQueue() {
        TransactionalLinkedList<Runnable> workQueue = new TransactionalLinkedList<Runnable>();
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor(workQueue);
        assertIsUnstarted(executor);
        assertSame(workQueue, executor.getWorkQueue());
    }

    @Test
    public void withPoolSize() {
        TransactionalThreadPoolExecutor executor = new TransactionalThreadPoolExecutor(10);
        assertEquals(10, executor.getCorePoolSize());
        assertIsUnstarted(executor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withPoolSize_whenPoolSizeNegative_thenIllegalArgumentException() {
        new TransactionalThreadPoolExecutor(-1);
    }
}
