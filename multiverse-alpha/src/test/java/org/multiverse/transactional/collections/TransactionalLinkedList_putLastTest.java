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

public class TransactionalLinkedList_putLastTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
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

        Transaction t = startUpdateTransaction(stm);
        setThreadLocalTransaction(t);

        try {
            list.putLast("3");
            fail();
        } catch (RetryError expected) {
        }
        t.abort();

        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }


}
