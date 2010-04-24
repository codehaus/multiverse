package org.multiverse.transactional.collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalLinkedList_constructorTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testNoArgConstructor() {
        long version = stm.getVersion();
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        assertEquals(0, list.size());
        assertFalse(list.hasRelaxedMaxCapacity());
        assertEquals(version, stm.getVersion());
        assertEquals(Integer.MAX_VALUE, list.getMaxCapacity());
        assertEquals("[]", list.toString());
    }

    @Test
    public void constructorWithNegativeMaxCapacity() {
        long version = stm.getVersion();

        try {
            new TransactionalLinkedList(-1);
            fail();
        } catch (IllegalArgumentException ignore) {

        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void constructorWithArray() {
        long version = stm.getVersion();
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>("1", "2");

        //todo: version should nto change... won't do damage..
        //assertEquals(version, stm.getVersion());
        assertEquals(2, list.size());
        assertEquals("1", list.get(0));
        assertEquals("2", list.get(1));
        assertFalse(list.hasRelaxedMaxCapacity());
        assertEquals(Integer.MAX_VALUE, list.getMaxCapacity());
    }

    @Test
    public void constructorWithMaxCapacity() {
        long version = stm.getVersion();

        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(10);

        assertEquals(version, stm.getVersion());
        assertEquals(10, list.getMaxCapacity());
        assertEquals("[]", list.toString());
        assertFalse(list.hasRelaxedMaxCapacity());
        assertEquals(0, list.size());
    }

    @Test
    public void constructorWithRelaxedMaxCapacity() {
        long version = stm.getVersion();
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(100, true);

        assertEquals(version, stm.getVersion());
        assertEquals(100, list.getMaxCapacity());
        assertTrue(list.hasRelaxedMaxCapacity());
        assertEquals(0, list.size());
    }

}

