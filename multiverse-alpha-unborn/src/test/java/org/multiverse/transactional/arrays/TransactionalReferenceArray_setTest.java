package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_setTest {
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

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooBig_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.set(10, "foo");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.set(-1, "foo");
    }

    @Test
    public void whenNoTransactionAvailable_thenExecutedAtomically() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        String update = "foo";
        long version = stm.getVersion();
        array.set(5, update);

        assertEquals(version + 1, stm.getVersion());
        assertSame(update, array.get(5));
    }

    @Test
    public void whenTransactionAvailable_thenThatTransactionIsUsed() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        setThreadLocalTransaction(tx);
        long version = stm.getVersion();
        array.set(1, "foo");
        tx.abort();

        assertEquals(version, stm.getVersion());
        assertNull(array.get(1));
    }

    @Test
    public void whenNoChange() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        long version = stm.getVersion();
        array.set(5, null);

        assertEquals(version, stm.getVersion());
        assertNull(array.atomicGet(5));
    }

    @Test
    @Ignore
    public void whenListenersAvailable_thenTheyAreNotified() {

    }
}
