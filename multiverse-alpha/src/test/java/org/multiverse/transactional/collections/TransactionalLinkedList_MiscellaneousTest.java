package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

import static junit.framework.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_MiscellaneousTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        clearThreadLocalTransaction();
    }

    // ================== roll back ==========================

    @Test
    public void rollbackPuts() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.put("1");
        long version = stm.getVersion();

        Transaction t = updateTxFactory.start();
        setThreadLocalTransaction(t);
        list.put("2");
        list.put("3");
        t.abort();

        assertEquals(version, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void rollbackTakes() throws InterruptedException {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.put("1");
        list.put("2");
        list.put("3");
        long version = stm.getVersion();

        Transaction t = updateTxFactory.start();
        setThreadLocalTransaction(t);
        list.take();
        list.take();
        t.abort();

        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2, 3]", list.toString());
    }

    // ========================================


    @Test
    public void testToString() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        assertEquals("[]", list.toString());

        list.add("1");
        assertEquals("[1]", list.toString());

        list.add("2");
        list.add("3");
        assertEquals("[1, 2, 3]", list.toString());
    }
}
