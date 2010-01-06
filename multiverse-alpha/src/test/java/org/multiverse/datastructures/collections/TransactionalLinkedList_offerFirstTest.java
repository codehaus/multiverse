package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_offerFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void offerFirstNullFails() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        long version = stm.getTime();

        try {
            deque.offerFirst(null);
            fail();
        } catch (NullPointerException ex) {

        }
        assertEquals(version, stm.getTime());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void offerFirstOnEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        long version = stm.getTime();

        boolean result = deque.offerFirst("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1]", deque.toString());
    }

    @Test
    public void offerFirstOnNonEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");

        long version = stm.getTime();
        boolean result = deque.offerFirst("3");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals("[3, 1, 2]", deque.toString());
    }

    @Test
    public void offerFirstOnFullDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>(1);
        deque.add("1");

        long version = stm.getTime();
        boolean result = deque.offerFirst("2");
        assertFalse(result);
        assertEquals(version, stm.getTime());
        assertEquals("[1]", deque.toString());
    }
}
