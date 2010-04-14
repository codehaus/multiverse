package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.Retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_takeFirstTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenNoItemsRetryErrorIsThrown() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        Transaction t = updateTxFactory.start();
        setThreadLocalTransaction(t);

        try {
            list.take();
            fail();
        } catch (Retry expected) {
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
