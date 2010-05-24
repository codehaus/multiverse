package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertNotEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.*;

public class TransactionTemplate_threadLocalTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenThreadLocalAwareAndNoThreadLocalTransaction_thenNewTransactionInstalled() {
        TransactionTemplate t = new TransactionTemplate(stm) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertSame(tx, getThreadLocalTransaction());
                return null;
            }
        };

        t.execute();
    }

    @Test
    public void whenThreadLocalAwareAndThreadLocalTransactionAvailable_thenNoChange() {
        final Transaction outerTx = stm.getTransactionFactoryBuilder()
                .build()
                .create();

        setThreadLocalTransaction(outerTx);

        new TransactionTemplate(stm) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertSame(outerTx, tx);
                assertSame(outerTx, getThreadLocalTransaction());
                return null;
            }
        }.execute();

        assertSame(outerTx, getThreadLocalTransaction());
    }

    @Test
    public void whenThreadLocalIgnoredAndTransactionActive_thenNewTransactionCreated() {
        final Transaction active = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(active);

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder().build();

        new TransactionTemplate<Transaction>(txFactory, false, false) {
            @Override
            public Transaction execute(Transaction tx) throws Exception {
                assertNotEquals(active, tx);
                assertSame(active, getThreadLocalTransaction());
                return tx;
            }
        }.execute();

        assertSame(active, getThreadLocalTransaction());
    }
}
