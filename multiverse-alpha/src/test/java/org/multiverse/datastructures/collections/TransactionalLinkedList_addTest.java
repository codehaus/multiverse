package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_addTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void addWithNullFails() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        try {
            deque.add(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(0, deque.size());
        assertEquals(version, stm.getTime());
    }

    @Test
    public void addOnEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        boolean result = deque.add("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1]", deque.toString());
    }

    @Test
    public void addOnNonEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");

        long version = stm.getTime();
        boolean result = deque.add("2");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1, 2]", deque.toString());
    }

    @Test
    public void addOnFullDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>(1);
        deque.add("1");

        long version = stm.getTime();

        try {
            deque.add("2");
            fail();
        } catch (IllegalStateException ex) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[1]", deque.toString());
        assertEquals(1, deque.size());
    }

}
