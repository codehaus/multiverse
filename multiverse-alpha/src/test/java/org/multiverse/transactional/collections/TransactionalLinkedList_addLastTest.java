package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_addLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void addLastWithNullFails() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        try {
            list.addLast(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(0, list.size());
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void addLastOnEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        list.addLast("1");
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void addLastOnNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.addLast("1");

        long version = stm.getVersion();
        list.addLast("2");
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }

    @Test
    public void addLastOnFullDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(1);
        list.add("1");

        long version = stm.getVersion();

        try {
            list.addLast("2");
            fail();
        } catch (IllegalStateException ex) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[1]", list.toString());
        assertEquals(1, list.size());
    }
}
