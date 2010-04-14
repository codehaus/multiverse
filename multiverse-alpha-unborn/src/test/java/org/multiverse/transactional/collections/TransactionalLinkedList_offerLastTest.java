package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_offerLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void offerLastNullFails() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();

        try {
            list.offerLast(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void offerLastOnEmptyQueue() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        boolean result = list.offerLast("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void offerLastOnNonEmptyQueue() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.offerLast("1");
        long version = stm.getVersion();

        boolean result = list.offerLast("2");

        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }

    @Test
    public void offerLastOnFullQueue() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(1);
        list.offerLast("1");
        long version = stm.getVersion();

        boolean result = list.offerLast("2");
        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void offerOnUnboundList() {
        TransactionalLinkedList<Integer> list = new TransactionalLinkedList<Integer>();

        long version = stm.getVersion();
        int txCount = 1000 * 1000;
        for (int k = 0; k < txCount; k++) {
            if (!list.offerLast(k)) {
                fail();
            }
        }

        assertEquals(version + txCount, stm.getVersion());
    }
}
