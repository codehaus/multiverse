package org.multiverse.api.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.collections.NaiveTransactionalStack;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.execute;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NaiveTransactionalStack_pollTest {

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
                String item = stack.poll();
                assertNull(item);
            }
        });
    }

    @Test
    public void whenSingleItem(){
         execute(new AtomicVoidClosure() {
             @Override
             public void execute(Transaction tx) throws Exception {
                 String item = "foo";
                 stack.push(item);

                 String found = stack.poll();
                 assertSame(item, found);
                 assertTrue(stack.isEmpty());
             }
         });
    }

     @Test
    public void whenMultipleItem(){
         execute(new AtomicVoidClosure() {
             @Override
             public void execute(Transaction tx) throws Exception {
                 String item1 = "foo";
                 String item2 = "foo";
                 stack.push(item1);
                 stack.push(item2);

                 String found = stack.poll();
                 assertSame(item2, found);
                 assertEquals(1, stack.size());
             }
         });
    }
}
