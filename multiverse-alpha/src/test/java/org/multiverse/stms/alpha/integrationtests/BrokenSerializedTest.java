package org.multiverse.stms.alpha.integrationtests;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransaction;
import org.multiverse.stms.alpha.manualinstrumentation.IntStack;
import org.multiverse.stms.alpha.manualinstrumentation.IntStackTranlocal;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * MVCC suffers from a problem that serialized transactions are not completely serialized.
 * For more information check the following blog.
 * http://pveentjer.wordpress.com/2008/10/04/breaking-oracle-serializable/
 * This tests makes sure that the error is still in there and that the stm behaves like 'designed'.
 * <p/>
 * The test:
 * There are 2 empty stacks. In 2 parallel transactions, the size of the one is pushed as element
 * on the second, and the size of the second is pushed as element on the first. Afer both transactions
 * have committed, the stm contains 2 stacks with both the element 0 on them. So not a 1 and 0, or 0
 * and 1 as true serialized execution of transactions would do.
 *
 * @author Peter Veentjer.
 */
public class BrokenSerializedTest {
    private IntStack stack1;
    private IntStack stack2;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
        stack1 = new IntStack();
        stack2 = new IntStack();
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void test() {
        AlphaTransaction t1 = stm.startUpdateTransaction(null);
        AlphaTransaction t2 = stm.startUpdateTransaction(null);

        IntStackTranlocal tranlocalStack1a = (IntStackTranlocal) t1.load(stack1);
        IntStackTranlocal tranlocalStack2a = (IntStackTranlocal) t1.load(stack2);
        stack1.push(tranlocalStack1a, stack2.size(tranlocalStack2a));

        IntStackTranlocal tranlocalStack1b = (IntStackTranlocal) t2.load(stack1);
        IntStackTranlocal tranlocalStack2b = (IntStackTranlocal) t2.load(stack2);
        stack2.push(tranlocalStack2b, stack1.size(tranlocalStack1b));

        t1.commit();
        t2.commit();

        assertStackContainsZero(stack1);
        assertStackContainsZero(stack2);
    }

    public void assertStackContainsZero(IntStack stack) {
        assertEquals(1, stack.size());
        assertEquals(0, stack.pop());
    }
}
