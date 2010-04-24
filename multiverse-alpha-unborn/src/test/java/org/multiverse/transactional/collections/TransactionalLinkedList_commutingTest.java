package org.multiverse.transactional.collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.OptimisticLockFailedWriteConflict;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_commutingTest {
    private Stm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadTrackingEnabled(true)
                .setReadonly(false)
                .build();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void test() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>("2", "3", "4");

        Transaction tx1 = txFactory.start();
        setThreadLocalTransaction(tx1);

        list.addFirst("1");

        Transaction tx2 = txFactory.start();
        setThreadLocalTransaction(tx2);

        list.addLast("5");

        tx1.commit();
        tx2.commit();

        assertEquals(5, list.size());
        assertEquals("[1, 2, 3, 4, 5]", list.toString());
    }

    @Test
    public void testNonCommuting() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>("2", "3", "4");

        Transaction tx1 = txFactory.start();
        setThreadLocalTransaction(tx1);

        list.addFirst("1");
        list.size();

        Transaction tx2 = txFactory.start();
        setThreadLocalTransaction(tx2);

        list.addLast("5");
        list.size();

        tx1.commit();

        try {
            tx2.commit();
            fail();
        } catch (OptimisticLockFailedWriteConflict expected) {

        }

        assertEquals(4, list.size());
        assertEquals("[1, 2, 3, 4]", list.toString());
    }
}
