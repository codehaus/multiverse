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

public class TransactionalLinkedList_takeFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenNoItemsRetryErrorIsThrown() throws InterruptedException {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        Transaction t = stm.startUpdateTransaction("");
        setThreadLocalTransaction(t);

        try {
            deque.take();
            fail();
        } catch (RetryError expected) {
        }
        t.abort();
        assertEquals(version, stm.getTime());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void whenSutIsSingletonSutBecomesEmpty() throws InterruptedException {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");

        long version = stm.getTime();
        String result = deque.takeFirst();
        assertEquals(version + 1, stm.getTime());
        assertEquals("1", result);
        assertEquals(0, deque.size());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void whenSutContainsMoreMultipleItemsFirstIsRemoved() throws InterruptedException {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");

        long version = stm.getTime();
        String result = deque.takeFirst();
        assertEquals(version + 1, stm.getTime());
        assertEquals("1", result);
        assertEquals(1, deque.size());
        assertEquals("[2]", deque.toString());
    }

}
