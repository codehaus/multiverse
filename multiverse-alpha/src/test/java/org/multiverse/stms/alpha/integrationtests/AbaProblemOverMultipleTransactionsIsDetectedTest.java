package org.multiverse.stms.alpha.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteConflictException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTestUtils;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;


public class AbaProblemOverMultipleTransactionsIsDetectedTest {

    private static final int A = 1;
    private static final int B = 2;
    private static final int C = 3;

    private AlphaStm stm;
    private IntRef ref;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
        ref = new IntRef(A);
    }

    public AlphaTransaction startUpdateTransaction() {
        AlphaTransaction t = AlphaTestUtils.startTrackingUpdateTransaction(stm);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void test() {
        AlphaTransaction t1 = startUpdateTransaction();
        IntRefTranlocal r1 = (IntRefTranlocal) t1.openForWrite(ref);

        AlphaTransaction t2 = startUpdateTransaction();
        IntRefTranlocal r2 = (IntRefTranlocal) t2.openForWrite(ref);
        ref.set(r2, B);
        t2.commit();

        AlphaTransaction t3 = startUpdateTransaction();
        IntRefTranlocal r3 = (IntRefTranlocal) t3.openForWrite(ref);
        ref.set(r3, B);
        t3.commit();

        ref.set(r1, C);
        try {
            t1.commit();
            fail();
        } catch (WriteConflictException er) {
        }
    }
}
