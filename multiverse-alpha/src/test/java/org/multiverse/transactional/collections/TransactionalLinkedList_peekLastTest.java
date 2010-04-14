package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_peekLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void peekLastOnEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        System.out.println("finished");
        long version = stm.getVersion();
        String result = list.peekLast();
        assertNull(result);
        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void peekLastOnNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        String result = list.peekLast();
        assertEquals("2", result);
        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }
}
