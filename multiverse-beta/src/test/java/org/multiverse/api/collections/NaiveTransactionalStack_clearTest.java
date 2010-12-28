package org.multiverse.api.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;

import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.execute;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NaiveTransactionalStack_clearTest {

    private Stm stm;
    private NaiveTransactionalStack<String> stack;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
        stack = new NaiveTransactionalStack<String>(stm);
    }

    @Test
    public void whenEmpty() {
        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                stack.clear();
                assertTrue(stack.isEmpty());
            }
        });
    }

    @Test
    public void whenNotEmpty(){
         execute(new AtomicVoidClosure() {
             @Override
             public void execute(Transaction tx) throws Exception {
                 stack.push("foo");
                 stack.push("foo");
                 stack.clear();
                 assertTrue(stack.isEmpty());
             }
         });
    }
}
