package org.multiverse.templates;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionTemplate_threadLocalTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void test() {
        TransactionTemplate t = new TransactionTemplate(stm) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                return null;
            }
        };

        assertTrue(t.isThreadLocalAware());
    }

    @Test
    public void whenThreadLocalIgnoredAndTransactionActive_thenNewTransactionCreated() {
        Transaction active = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(active);

        Transaction found = new TransactionTemplate<Transaction>(stm.getTransactionFactoryBuilder().build(), false, false) {
            @Override
            public Transaction execute(Transaction tx) throws Exception {
                return tx;
            }
        }.execute();

        assertNotNull(found);
        assertNotSame(active, found);
        assertSame(active, getThreadLocalTransaction());
    }
}
