package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.Transactions;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AbaRefTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    public Transaction startTransaction() {
        Transaction t = Transactions.startUpdateTransaction(stm);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testAbaProblemIsDetected() {
        final String a = "A";
        final String b = "B";

        final AbaRef<String> ref = new AbaRef<String>(a);

        long startVersion = stm.getVersion();
        Transaction t = startTransaction();
        ref.set(b);
        ref.set(a);
        t.commit();
        assertEquals(startVersion + 1, stm.getVersion());
    }

}
