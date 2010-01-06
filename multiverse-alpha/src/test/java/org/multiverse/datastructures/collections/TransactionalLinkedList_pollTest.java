package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_pollTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenSutIsEmptyNullIsReturned() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        String result = deque.poll();
        assertNull(result);
        assertEquals(version, stm.getTime());
        assertEquals(0, deque.size());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void whenSutIsSingletonSetBecomesEmpty() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");

        long version = stm.getTime();
        String result = deque.poll();
        assertEquals("1", result);
        assertEquals(version + 1, stm.getTime());
        assertEquals(0, deque.size());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void whenSutContainsMultipleItemsFirstIsRemoved() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");

        long version = stm.getTime();
        String result = deque.poll();
        assertEquals("1", result);
        assertEquals(version + 1, stm.getTime());
        assertEquals(1, deque.size());
        assertEquals("[2]", deque.toString());
    }

}
