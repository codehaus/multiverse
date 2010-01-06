package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.RetryError;

import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_putLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenNullItemNullPointerIsThrown() throws InterruptedException {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();

        try {
            deque.putLast(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void whenEmptyQueueItemIsAddedSuccessfully() throws InterruptedException {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        long version = stm.getTime();

        deque.putLast("1");
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1]", deque.toString());
    }

    @Test
    public void whenQueueIsNotEmptyItemIsPlacedAndTheEnd() throws InterruptedException {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        long version = stm.getTime();

        deque.put("2");

        assertEquals(version + 1, stm.getTime());
        assertEquals("[1, 2]", deque.toString());
    }

    @Test
    public void whenQueueIsFullRetryErrorIsThrown() throws InterruptedException {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>(2);
        deque.put("1");
        deque.put("2");

        long version = stm.getTime();

        Transaction t = stm.startUpdateTransaction("");
        setThreadLocalTransaction(t);

        try {
            deque.putLast("3");
            fail();
        } catch (RetryError expected) {
        }
        t.abort();

        assertEquals(version, stm.getTime());
        assertEquals("[1, 2]", deque.toString());
    }


}
