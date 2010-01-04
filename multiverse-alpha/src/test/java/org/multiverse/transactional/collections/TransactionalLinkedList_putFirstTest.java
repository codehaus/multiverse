package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.api.Transactions.startUpdateTransaction;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.RetryError;

public class TransactionalLinkedList_putFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenNullItemNullPointerException() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();

        try {
            list.putFirst(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenListIsEmptyItemBecomesTheSingleContent() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        list.putFirst("1");
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void whenListIsNonEmptyItemIsAddedToFront() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        long version = stm.getVersion();

        list.putFirst("2");

        assertEquals(version + 1, stm.getVersion());
        assertEquals("[2, 1]", list.toString());
    }

    @Test
    public void whenListIsFullRetryErrorIsThrown() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(2);
        list.put("1");
        list.put("2");

        long version = stm.getVersion();

        Transaction t = startUpdateTransaction(stm);
        setThreadLocalTransaction(t);

        try {
            list.putFirst("3");
            fail();
        } catch (RetryError expected) {
        }
        t.abort();

        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }
}
