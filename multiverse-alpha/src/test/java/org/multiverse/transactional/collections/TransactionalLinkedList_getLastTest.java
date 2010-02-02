package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.NoSuchElementException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_getLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void getLastFromEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();

        try {
            list.getLast();
            fail();
        } catch (NoSuchElementException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void getLastFromSingleElementDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        long version = stm.getVersion();

        String result = list.getLast();

        assertEquals("1", result);
        assertEquals(version, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void getLastFromNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        long version = stm.getVersion();

        String result = list.getLast();

        assertEquals("2", result);
        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }
}
