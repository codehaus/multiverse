package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_lastIndexOfTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void lastIndexOfNullFails() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        try {
            list.lastIndexOf(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void lastIndexOf() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("1");

        long version = stm.getVersion();
        assertEquals(3, list.lastIndexOf("1"));
        assertEquals(1, list.lastIndexOf("2"));
        assertEquals(2, list.lastIndexOf("3"));
        assertEquals(-1, list.lastIndexOf("a"));
        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2, 3, 1]", list.toString());
    }
}
