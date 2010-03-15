package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.Retry;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_takeLastTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenEmptyRetryErrorIsThrown() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        Transaction t = updateTxFactory.start();
        setThreadLocalTransaction(t);

        try {
            list.takeLast();
            fail();
        } catch (Retry expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenSingletonTheItemIsTaken() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        long version = stm.getVersion();
        String result = list.takeLast();
        assertEquals("1", result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenMultipleItemsLastItemIsTaken() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        String result = list.takeLast();
        assertEquals("2", result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[1]", list.toString());
    }
}
