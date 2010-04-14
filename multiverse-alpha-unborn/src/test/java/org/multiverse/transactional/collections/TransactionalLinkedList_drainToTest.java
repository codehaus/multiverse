package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_drainToTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void drainToWithEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        Collection<String> c = new TransactionalLinkedList<String>();
        int result = list.drainTo(c);
        assertEquals(0, result);

        assertTrue(c.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void drainToWithNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        Collection<String> c = new TransactionalLinkedList<String>();
        long version = stm.getVersion();
        int result = list.drainTo(c);
        assertEquals(3, result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1, 2, 3]", c.toString());
        assertEquals(0, list.size());
    }
}
