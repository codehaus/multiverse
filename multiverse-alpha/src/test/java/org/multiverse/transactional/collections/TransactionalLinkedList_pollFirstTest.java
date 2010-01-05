package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_pollFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void pollFirstFromEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        String result = list.pollFirst();
        assertEquals(version, stm.getVersion());
        assertNull(result);
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void pollFirstFromDequeWithSingleItem() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        long version = stm.getVersion();
        String result = list.pollFirst();
        assertEquals(version + 1, stm.getVersion());
        assertEquals("1", result);
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void pollFirstFromDequeWithMultipleItems() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        String result = list.pollFirst();
        assertEquals("1", result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[2]", list.toString());
    }
}
