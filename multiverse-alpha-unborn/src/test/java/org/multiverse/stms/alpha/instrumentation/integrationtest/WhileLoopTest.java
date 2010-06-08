package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class WhileLoopTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenUsedInExpression() {
        IntRef ref = new IntRef(0);

        int result = expressionInWhileLoop(ref);
        assertEquals(10, result);
    }

    @TransactionalMethod(readonly = false)
    public int expressionInWhileLoop(IntRef ref) {
        int result = 0;
        while (ref.get() < 10 && result < 10) {
            result++;
        }

        return result;
    }

    @Test
    public void whenUsedInLoop() {
        IntRef ref = new IntRef();
        referenceInWhileLoop(ref);
        assertEquals(10, ref.get());
    }

    @TransactionalMethod(readonly = false)
    public void referenceInWhileLoop(IntRef ref) {
        int x = 0;
        while (x < 10) {
            ref.inc();
            x++;
        }
    }
}
