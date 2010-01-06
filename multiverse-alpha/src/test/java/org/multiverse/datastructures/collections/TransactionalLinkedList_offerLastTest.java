package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_offerLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void offerLastNullFails() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();

        try {
            deque.offerLast(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void offerLastOnEmptyQueue() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        long version = stm.getTime();

        boolean result = deque.offerLast("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1]", deque.toString());
    }

    @Test
    public void offerLastOnNonEmptyQueue() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.offerLast("1");
        long version = stm.getTime();

        boolean result = deque.offerLast("2");

        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1, 2]", deque.toString());
    }

    @Test
    public void offerLastOnFullQueue() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>(1);
        deque.offerLast("1");
        long version = stm.getTime();

        boolean result = deque.offerLast("2");
        assertFalse(result);
        assertEquals(version, stm.getTime());
        assertEquals("[1]", deque.toString());
    }
}
