package org.multiverse.stms.alpha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;

/**
 * @author Peter Veentjer
 */
public class UpdateAlphaTransaction_abortAndReturnRestartedTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @Test
    public void callActiveTransaction() {
        Transaction t = stm.startUpdateTransaction(null);

        //commit some dummy change
        new IntRef(20);

        Transaction result = t.abortAndReturnRestarted();
        assertSame(result, t);
        assertIsActive(t);
        assertEquals(stm.getTime(), t.getReadVersion());
    }

    @Test
    public void callAbortedTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
        t.abort();

        //commit some dummy change
        new IntRef(20);

        Transaction result = t.abortAndReturnRestarted();
        assertSame(result, t);
        assertIsActive(t);
        assertEquals(stm.getTime(), t.getReadVersion());
    }

    @Test
    public void callCommittedTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
        t.commit();

        //commit some dummy change
        new IntRef(20);

        Transaction result = t.abortAndReturnRestarted();
        assertSame(result, t);
        assertIsActive(t);
        assertEquals(stm.getTime(), t.getReadVersion());
    }
}
