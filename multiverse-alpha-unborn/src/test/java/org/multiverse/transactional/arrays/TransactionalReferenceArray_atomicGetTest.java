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
public class TransactionalReferenceArray_atomicGetTest {
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
        array.atomicGet(10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.atomicGet(-1);
    }

    @Test
    public void test() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        int index = 5;
        long version = stm.getVersion();
        array.atomicGet(index);
        assertEquals(version, stm.getVersion());

        String update = "foo";
        array.atomicSet(index, update);

        version = stm.getVersion();
        assertSame(update, array.atomicGet(index));
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenTransactionAvailable_thenIgnored() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        setThreadLocalTransaction(tx);
        array.set(5, "foo");

        assertNull(array.atomicGet(5));
    }
}
