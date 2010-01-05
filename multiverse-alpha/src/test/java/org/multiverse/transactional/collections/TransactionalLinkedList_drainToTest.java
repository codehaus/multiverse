package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.Collection;
import java.util.LinkedList;

public class TransactionalLinkedList_drainToTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    @Ignore
    public void drainToWithEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        Collection<String> c = new LinkedList<String>();
        long version = stm.getVersion();
        int result = list.drainTo(c);
        assertEquals(0, result);

        assertEquals(version, stm.getVersion());
        assertTrue(c.isEmpty());
        assertEquals(0, list.size());
        testIncomplete();
    }

    @Ignore
    @Test
    public void drainToWithNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        Collection<String> c = new LinkedList<String>();
        long version = stm.getVersion();
        int result = list.drainTo(c);
        assertEquals(3, result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1, 2, 3]", c.toString());
        assertEquals(0, list.size());
        testIncomplete();
    }
}
