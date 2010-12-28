package org.multiverse.api.collections;


import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NaiveTransactionalStack_offerTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }


    @Test
    public void whenNullItem_thenNullPointerException(){

    }
}
