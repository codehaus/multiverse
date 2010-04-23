package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
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
public class TransactionalReferenceArray_getTest {
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
        array.get(10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.get(-1);
    }

    @Test
    public void whenNoTransactionAvailable() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        long version = stm.getVersion();
        String value = array.get(5);

        assertNull(value);
        assertEquals(version, stm.getVersion());

        String update = "bar";
        array.atomicSet(5, update);

        version = stm.getVersion();
        assertSame(update, array.get(5));
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenTransactionActive() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .start();
        setThreadLocalTransaction(tx);

        String update = "bar";
        array.set(5, update);
        long version = stm.getVersion();
        String found = array.get(5);

        assertSame(update, found);
        assertEquals(version, stm.getVersion());
    }
}
