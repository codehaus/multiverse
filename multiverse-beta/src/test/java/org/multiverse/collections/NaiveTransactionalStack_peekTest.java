package org.multiverse.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.collections.NaiveTransactionalStack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.execute;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NaiveTransactionalStack_peekTest {

    private Stm stm;
    private NaiveTransactionalStack<String> stack;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
        stack = new NaiveTransactionalStack<String>(stm);
    }

    @Test
    public void whenEmpty(){
        execute(new AtomicVoidClosure(){
            @Override
            public void execute(Transaction tx) throws Exception {
                String s = stack.peek();
                assertNull(s);
                assertEquals("[]", stack.toString());
            }
        });
    }

    @Test
    public void whenNotEmpty(){
        execute(new AtomicVoidClosure(){
            @Override
            public void execute(Transaction tx) throws Exception {
                String item1 = "foo";
                String item2 = "bar";
                stack.push(item1);
                stack.push(item2);
                String s = stack.peek();
                assertSame(item2,s);
                assertEquals("[bar, foo]", stack.toString());
            }
        });
    }
}
