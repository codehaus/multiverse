package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.Test;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class IfElseTest {

    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenUsedInExpression() {
        assertEquals(1, methodWhereUsedInCondition(new IntRef(0)));
        assertEquals(2, methodWhereUsedInCondition(new IntRef(1)));
    }

    public int methodWhereUsedInCondition(IntRef ref) {
        if (ref.get() == 0) {
            return 1;
        } else {
            return 2;
        }
    }

    @Test
    public void whenUsedInIfBranch() {
        IntRef ref = new IntRef();
        methodWhereUsedInBranch(true, ref);
        assertEquals(1, ref.get());
    }

    @Test
    public void whenUsedInElseBranch() {
        IntRef ref = new IntRef();
        methodWhereUsedInBranch(false, ref);
        assertEquals(2, ref.get());
    }

    public void methodWhereUsedInBranch(boolean condition, IntRef ref){
        if(condition){
            ref.set(1);
        }else{
            ref.set(2);
        }
    }

}