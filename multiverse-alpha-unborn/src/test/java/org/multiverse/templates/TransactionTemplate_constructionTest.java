package org.multiverse.templates;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionTemplate_constructionTest {
    private Stm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();
    }

    @Test
    public void testNoArg() {
        TransactionTemplate t = new TransactionTemplate() {
            @Override
            public Object execute(Transaction tx) throws Exception {
                return null;  //todo
            }
        };

        assertNotNull(t.getTransactionFactory());
        assertTrue(t.isThreadLocalAware());
        assertTrue(t.isLifecycleListenersEnabled());
    }

    @Test
    public void testWithTxFactory() {
        TransactionFactory factory = stm.getTransactionFactoryBuilder()
                .build();

        TransactionTemplate t = new TransactionTemplate(factory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                return null;  //todo
            }
        };

        assertSame(factory, t.getTransactionFactory());
        assertTrue(t.isThreadLocalAware());
        assertTrue(t.isLifecycleListenersEnabled());
    }
}
