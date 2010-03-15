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

public class TransactionalLinkedList_putLastTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenNullItemNullPointerIsThrown() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();

        try {
            list.putLast(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenEmptyQueueItemIsAddedSuccessfully() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        list.putLast("1");
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void whenQueueIsNotEmptyItemIsPlacedAndTheEnd() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        long version = stm.getVersion();

        list.put("2");

        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }

    @Test
    public void whenQueueIsFullRetryErrorIsThrown() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(2);
        list.put("1");
        list.put("2");

        long version = stm.getVersion();

        Transaction t = updateTxFactory.start();
        setThreadLocalTransaction(t);

        try {
            list.putLast("3");
            fail();
        } catch (Retry expected) {
        }
        t.abort();

        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }


}
