package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_peekLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void peekLastOnEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();

        System.out.println("finished");
        long version = stm.getTime();
        String result = deque.peekLast();
        assertNull(result);
        assertEquals(version, stm.getTime());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void peekLastOnNonEmptyDeque() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");

        long version = stm.getTime();
        String result = deque.peekLast();
        assertEquals("2", result);
        assertEquals(version, stm.getTime());
        assertEquals("[1, 2]", deque.toString());
    }
}
