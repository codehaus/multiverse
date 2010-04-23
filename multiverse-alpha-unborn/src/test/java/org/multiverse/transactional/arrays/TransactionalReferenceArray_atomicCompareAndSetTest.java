package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_atomicCompareAndSetTest {
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
        array.atomicCompareAndSet(10, "foo", "bar");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.atomicCompareAndSet(-1, "foo", "bar");
    }

    @Test
    public void whenExpectedValueFound() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(10);

        long version = stm.getVersion();
        String update = "foo";
        boolean result = array.atomicCompareAndSet(5, null, update);

        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(update, array.atomicGet(5));
    }

    @Test
    public void whenExpectedValueNotFound() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(10);

        long version = stm.getVersion();
        boolean result = array.atomicCompareAndSet(5, "banana", "foo");

        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertNull(array.atomicGet(5));
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(10);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .start();

        setThreadLocalTransaction(tx);

        long version = stm.getVersion();
        boolean result = array.atomicCompareAndSet(5, "banana", "foo");

        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertNull(array.atomicGet(5));
        assertIsActive(tx);
    }
}
