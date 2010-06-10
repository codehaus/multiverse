package org.multiverse.stms.alpha.instrumentation.integrationtest.languageconstructs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class SwitchCaseTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenUsedInExpression() {
        assertEquals(10, usedInExpression(new IntRef(1)));
        assertEquals(20, usedInExpression(new IntRef(2)));
        assertEquals(30, usedInExpression(new IntRef(3)));
    }

    @TransactionalMethod
    public int usedInExpression(IntRef ref) {
        switch (ref.get()) {
            case 1:
                return 10;
            case 2:
                return 20;
            default:
                return 30;
        }
    }

    @Test
    public void whenUsedInStatement() {
        IntRef ref = new IntRef();
        usedInStatement(0, ref);
        assertEquals(10, ref.get());

        ref.set(0);
        usedInStatement(1, ref);
        assertEquals(20, ref.get());

        ref.set(0);
        usedInStatement(3, ref);
        assertEquals(30, ref.get());
    }

    @TransactionalMethod
    public void usedInStatement(int value, IntRef ref) {
        switch (value) {
            case 0:
                ref.set(10);
                break;
            case 1:
                ref.set(20);
                break;
            default:
                ref.set(30);
                break;
        }
    }
}