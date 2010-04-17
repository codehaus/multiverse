package org.multiverse.stms.alpha.instrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionAccessTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        MyInt ref = new MyInt();
        System.out.println("------------------------------------------");
        ref.inc(100);
        System.out.println("------------------------------------------");

        assertEquals(100, ref.get());
    }

    @TransactionalObject
    class MyInt {

        private int value;

        public int get() {
            return value;
        }

        public void inc(int amount) {
            for (int k = 0; k < amount; k++) {
                value = value + 1;
            }
        }
    }
}
