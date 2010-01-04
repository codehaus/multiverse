package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A Test that checks if the system is able to deal with nested transactions. The scenario is a recursive transactional
 * method/atomictemplate that is called and therefor causes nesting
 *
 * @author Peter Veentjer.
 */
public class NestedTransactionTest {

    private TransactionalInteger ref;
    private int maximumDepth = 50;
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        ref = new TransactionalInteger();
    }

    @Test
    public void testTransactionalMethod() {
        long version = stm.getVersion();
        beginTxMethod();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, ref.get());
    }

    @TransactionalMethod
    public void beginTxMethod() {
        Transaction t = getThreadLocalTransaction();
        recursiveTxMethod(0, t);
    }

    //It is important that the expectedTransaction is the last argument. The instrumentation also creates an
    //extra method with the transaction as introduced argument as the first argument.

    @TransactionalMethod
    public void recursiveTxMethod(int currentDepth, Transaction expectedTransaction) {
        Transaction found = getThreadLocalTransaction();
        assertSame(expectedTransaction, found);

        if (currentDepth == maximumDepth) {
            ref.inc();
        } else {
            recursiveTxMethod(currentDepth + 1, found);
        }
    }

    @Test
    public void testTransactionTemplate() {
        long version = stm.getVersion();
        beginTxTemplate();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, ref.get());
    }

    public void beginTxTemplate() {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false).build();

        new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction t) throws Exception {
                recursiveTxTemplate(0, t);
                return null;
            }
        }.execute();
    }

    public void recursiveTxTemplate(final int currentDepth, final Transaction expectedTransaction) {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder().build();

        new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction t) throws Exception {
                Transaction found = getThreadLocalTransaction();

                assertSame(expectedTransaction, found);

                if (currentDepth == maximumDepth) {
                    ref.inc();
                } else {
                    recursiveTxTemplate(currentDepth + 1, found);
                }
                return null;
            }

        }.execute();
    }
}
