package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_indexOfTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void indexOfNullFails() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        try {
            list.indexOf(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void indexOf() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("1");

        long version = stm.getVersion();
        assertEquals(0, list.indexOf("1"));
        assertEquals(1, list.indexOf("2"));
        assertEquals(2, list.indexOf("3"));
        assertEquals(-1, list.indexOf("a"));
        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2, 3, 1]", list.toString());
    }
}
