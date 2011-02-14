package org.multiverse.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NaiveTransactionalStack_removeTest {

    private Stm stm;
    private NaiveTransactionalStack<String> stack;

    @Before
    public void setUp(){
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
        stack = new NaiveTransactionalStack<String>(stm);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void whenCalled_thenUnsupportedOperationException(){
        stack.remove("foo");
    }
}
