package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_clearTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void clearEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        list.clear();
        assertEquals(version, stm.getVersion());
        assertTrue(list.isEmpty());
        assertEquals("[]", list.toString());
    }

    @Test
    public void clearNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        long version = stm.getVersion();
        list.clear();
        assertEquals(version + 1, stm.getVersion());
        assertTrue(list.isEmpty());
        assertEquals("[]", list.toString());
    }
}
