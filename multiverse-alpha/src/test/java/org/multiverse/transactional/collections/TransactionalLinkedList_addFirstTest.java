package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_addFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void addFirstWithNullFails() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        try {
            list.addFirst(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void addFirstOnEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        list.addFirst("1");

        assertEquals(1, list.size());
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void addFirstOnNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.addFirst("1");

        long version = stm.getVersion();

        list.addFirst("2");
        assertEquals(version + 1, stm.getVersion());
        assertEquals(2, list.size());
        assertEquals("[2, 1]", list.toString());
    }

    @Test
    public void addFirstWithFullDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(1);
        list.addFirst("1");

        long version = stm.getVersion();
        try {
            list.addFirst("2");
            fail();
        } catch (IllegalStateException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[1]", list.toString());
    }
}
