package org.multiverse.datastructures.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalLinkedList_constructorTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void testNoArgConstructor() {
        long version = stm.getTime();
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();

        assertEquals(version + 1, stm.getTime());
        assertEquals(Integer.MAX_VALUE, deque.getMaxCapacity());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void constructorWithNegativeMaxCapacity() {
        long version = stm.getTime();

        try {
            new TransactionalLinkedList(-1);
            fail();
        } catch (IllegalArgumentException ignore) {

        }

        assertEquals(version, stm.getTime());
    }

    @Test
    public void constructorWithMaxCapacity() {
        long version = stm.getTime();

        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>(10);

        assertEquals(version + 1, stm.getTime());
        assertEquals(10, deque.getMaxCapacity());
        assertEquals("[]", deque.toString());
    }
}

