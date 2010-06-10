package org.multiverse.stms.alpha.instrumentation.integrationtest.languageconstructs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.collections.TransactionalArrayList;
import org.multiverse.transactional.collections.TransactionalList;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ForLoopTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenUsedInExpression() {
        int result = usedInExpression();
        assertEquals(10, result);
    }

    @TransactionalMethod
    public int usedInExpression() {
        int result = 0;
        for (IntRef ref = new IntRef(); ref.get() < 10; ref.inc()) {
            result++;
        }
        return result;
    }

    @Test
    public void whenUsedInsideLoop() {
        IntRef ref = new IntRef();
        usedInLoop(ref);
        assertEquals(10, ref.get());
    }

    @TransactionalMethod
    public void usedInLoop(IntRef ref) {
        for (int k = 0; k < 10; k++) {
            ref.inc();
        }
    }

    @Test
    public void whenUsedInNewForLoop() {
        TransactionalList<String> list = new TransactionalArrayList<String>("a", "b", "c");
        String result = newForLoop(list);
        assertEquals("abc", result);
    }

    @TransactionalMethod
    public String newForLoop(TransactionalList<String> list) {
        String result = "";
        for (String item : list) {
            result+=item;
        }
        return result;
    }

}
