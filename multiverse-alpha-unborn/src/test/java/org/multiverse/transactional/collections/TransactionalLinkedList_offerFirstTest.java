package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_offerFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void offerFirstNullFails() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        try {
            list.offerFirst(null);
            fail();
        } catch (NullPointerException ex) {

        }
        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void offerFirstOnEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        boolean result = list.offerFirst("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void offerFirstOnNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        boolean result = list.offerFirst("3");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[3, 1, 2]", list.toString());
    }

    @Test
    public void offerFirstOnFullDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(1);
        list.add("1");

        long version = stm.getVersion();
        boolean result = list.offerFirst("2");
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
            if (!list.offerFirst(k)) {
                fail();
            }
        }

        assertEquals(version + txCount, stm.getVersion());
    }
}
