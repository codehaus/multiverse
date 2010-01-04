package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_addTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void addWithNullFails() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        try {
            list.add(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(0, list.size());
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void addOnEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        boolean result = list.add("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void addOnNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        long version = stm.getVersion();
        boolean result = list.add("2");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }

    @Test
    public void addOnFullDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(1);
        list.add("1");

        long version = stm.getVersion();

        try {
            list.add("2");
            fail();
        } catch (IllegalStateException ex) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[1]", list.toString());
        assertEquals(1, list.size());
    }

}
