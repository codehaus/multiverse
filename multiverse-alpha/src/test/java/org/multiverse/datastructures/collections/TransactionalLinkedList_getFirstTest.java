package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_getFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void getFirstFromEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();

        try {
            deque.getFirst();
            fail();
        } catch (NoSuchElementException expected) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void getFirstFromSingleElementDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        long version = stm.getTime();

        String result = deque.getFirst();

        assertEquals("1", result);
        assertEquals(version, stm.getTime());
        assertEquals("[1]", deque.toString());
    }

    @Test
    public void getFirstFromNonEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");
        long version = stm.getTime();

        String result = deque.getFirst();

        assertEquals("1", result);
        assertEquals(version, stm.getTime());
        assertEquals("[1, 2]", deque.toString());
    }
}
