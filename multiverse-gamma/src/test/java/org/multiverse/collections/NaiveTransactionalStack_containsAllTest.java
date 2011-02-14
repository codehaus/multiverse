package org.multiverse.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;

import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.execute;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NaiveTransactionalStack_containsAllTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNullCollection_thenNullPointerException() {
        final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                try {
                    stack.containsAll(null);
                    fail();
                } catch (NullPointerException expected) {

                }

                assertEquals("[]", stack.toString());
                assertEquals(0, stack.size());
            }
        });
    }

    @Test
    public void whenBothEmpty() {
        final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                boolean result = stack.containsAll(new LinkedList());

                assertTrue(result);
                assertEquals("[]", stack.toString());
                assertEquals(0, stack.size());
            }
        });
    }

    @Test
    public void whenStackEmpty_andCollectionNonEmpty() {
       final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                boolean result = stack.containsAll(Arrays.asList("1","2"));

                assertFalse(result);
                assertEquals("[]", stack.toString());
                assertEquals(0, stack.size());
            }
        });
    }

    @Test
    public void whenStackNonEmpty_andCollectionEmpty() {
       final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                stack.push("1");
                stack.push("2");

                boolean result = stack.containsAll(new LinkedList<String>());

                assertTrue(result);
                assertEquals("[2, 1]", stack.toString());
                assertEquals(2, stack.size());
            }
        });
    }

    @Test
    public void whenExactMatch() {
         final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                stack.add("1");
                stack.add("2");
                boolean result = stack.containsAll(Arrays.asList("1","2"));

                assertTrue(result);
                assertEquals("[2, 1]", stack.toString());
                assertEquals(2, stack.size());
            }
        });
    }

    @Test
    public void whenOrderDifferentThanStillMatch() {
          final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                stack.add("1");
                stack.add("2");
                stack.add("1");
                boolean result = stack.containsAll(Arrays.asList("1","2"));

                assertTrue(result);
                assertEquals("[1, 2, 1]", stack.toString());
                assertEquals(3, stack.size());
            }
        });
    }

    @Test
    public void whenNoneMatch() {
         final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                stack.add("1");
                stack.add("2");
                stack.add("3");
                boolean result = stack.containsAll(Arrays.asList("a","b"));

                assertFalse(result);
                assertEquals("[3, 2, 1]", stack.toString());
                assertEquals(3, stack.size());
            }
        });
    }

    @Test
    public void whenSomeMatch() {
             final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                stack.add("1");
                stack.add("2");
                stack.add("3");
                boolean result = stack.containsAll(Arrays.asList("1","b"));

                assertFalse(result);
                assertEquals("[3, 2, 1]", stack.toString());
                assertEquals(3, stack.size());
            }
        });
    }

    @Test
    public void whenSomeElementsNull() {
        final NaiveTransactionalStack<String> stack = new NaiveTransactionalStack<String>(stm);

          execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                stack.add("1");
                stack.add("2");
                stack.add("3");
                boolean result = stack.containsAll(Arrays.asList("1", null));

                assertFalse(result);
                assertEquals("[3, 2, 1]", stack.toString());
                assertEquals(3, stack.size());
            }
        });
    }
}
