package org.multiverse.transactional.collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.NoSuchElementException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalLinkedList_removeFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown(){
        clearThreadLocalTransaction();
    }

    @Test
    public void removeFirstFromEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        try {
            list.removeFirst();
            fail();
        } catch (NoSuchElementException expected) {

        }
        assertEquals(0, list.size());
        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void removeFirstFromDequeWithSingleItem() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        long version = stm.getVersion();
        String result = list.removeFirst();
        assertEquals("1", result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void removeFirstFromDequeWithMultipleItems() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        String result = list.removeFirst();
        assertEquals("1", result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[2]", list.toString());
    }

}
