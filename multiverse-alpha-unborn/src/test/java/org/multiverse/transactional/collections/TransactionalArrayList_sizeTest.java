package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalArrayList_sizeTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenEmpty() {
        TransactionalArrayList<String> array = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        int result = array.size();

        assertEquals(0, result);
        assertEquals(version, stm.getVersion());
    }
}
