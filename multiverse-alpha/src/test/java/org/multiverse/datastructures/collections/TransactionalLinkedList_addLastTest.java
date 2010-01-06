package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_addLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void addLastWithNullFails() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        try {
            deque.addLast(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(0, deque.size());
        assertEquals(version, stm.getTime());
    }

    @Test
    public void addLastOnEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        deque.addLast("1");
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1]", deque.toString());
    }

    @Test
    public void addLastOnNonEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.addLast("1");

        long version = stm.getTime();
        deque.addLast("2");
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1, 2]", deque.toString());
    }

    @Test
    public void addLastOnFullDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>(1);
        deque.add("1");

        long version = stm.getTime();

        try {
            deque.addLast("2");
            fail();
        } catch (IllegalStateException ex) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[1]", deque.toString());
        assertEquals(1, deque.size());
    }
}
