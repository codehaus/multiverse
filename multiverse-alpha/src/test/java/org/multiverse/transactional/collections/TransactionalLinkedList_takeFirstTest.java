package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

import org.multiverse.api.Transaction;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.api.Transactions.startUpdateTransaction;

import org.multiverse.api.exceptions.RetryError;

public class TransactionalLinkedList_takeFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenNoItemsRetryErrorIsThrown() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        Transaction t = startUpdateTransaction(stm);
        setThreadLocalTransaction(t);

        try {
            list.take();
            fail();
        } catch (RetryError expected) {
        }
        t.abort();
        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenSutIsSingletonSutBecomesEmpty() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        long version = stm.getVersion();
        String result = list.takeFirst();
        assertEquals(version + 1, stm.getVersion());
        assertEquals("1", result);
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenSutContainsMoreMultipleItemsFirstIsRemoved() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        String result = list.takeFirst();
        assertEquals(version + 1, stm.getVersion());
        assertEquals("1", result);
        assertEquals(1, list.size());
        assertEquals("[2]", list.toString());
    }

}
