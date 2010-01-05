package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_pollLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void pollLastFromEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        String result = list.pollLast();
        assertNull(result);
        assertEquals(version, stm.getVersion());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void pollLastFromDequeWithSingleItem() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        long version = stm.getVersion();
        String result = list.pollLast();
        assertEquals("1", result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void pollLastFromDequeWithMultipleItems() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        String result = list.pollLast();
        assertEquals("2", result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[1]", list.toString());
    }


}
