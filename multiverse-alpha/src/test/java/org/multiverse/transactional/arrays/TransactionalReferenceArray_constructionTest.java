package org.multiverse.transactional.arrays;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalReferenceArray_constructionTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }


    @Test(expected = IllegalArgumentException.class)
    public void whenSizeNegative_thenNegativeArraySizeException() {
        new TransactionalReferenceArray<String>(-1);
    }

    @Test
    public void whenConstructed() {
        long version = stm.getVersion();

        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        assertEquals(10, array.length());
        assertEquals(version, stm.getVersion());

        for (int k = 0; k < 10; k++) {
            assertNull(array.atomicGet(k));
        }
    }
}
