package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.transactions.readonly.NonTrackingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.GrowingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.GrowingUpdateAlphaTransaction;

import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaStmTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm)getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void testDefaultTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder().build().start();
        assertTrue(t instanceof NonTrackingReadonlyAlphaTransaction);

        //parameters should be checked
        testIncomplete();
    }

    @Test
    public void testDefaultUpdateTransaction(){
        Transaction t = stm.getTransactionFactoryBuilder().setReadonly(false).build().start();
        assertTrue(t instanceof GrowingUpdateAlphaTransaction);

        //parameters should be checked
        testIncomplete();
    }

    @Test
    public void test(){
        Transaction t = stm.getTransactionFactoryBuilder().setAutomaticReadTracking(true).build().start();
        assertTrue(t instanceof GrowingReadonlyAlphaTransaction);

        //parameters should be checked
        testIncomplete();
        
    }

    @Test
    public void testNonTrackingUpdate() {
        Transaction t = stm.getTransactionFactoryBuilder().setReadonly(false).setAutomaticReadTracking(true).build().start();
        assertTrue(t instanceof GrowingUpdateAlphaTransaction);
    }
}
