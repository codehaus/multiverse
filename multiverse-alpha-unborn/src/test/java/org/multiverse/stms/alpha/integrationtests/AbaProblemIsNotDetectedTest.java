package org.multiverse.stms.alpha.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * A test that shows that the ABA problem is not detected.
 * <p/>
 * The ABA problem occurrs when the following sequence happens:
 * <ol>
 * <li>a transaction reads a value "A" and doesn't commit yet</li>
 * <li>a second transaction udates the value to "B"<li>
 * <li>a third transaction updates the value to "A"</li>
 * <li>the first transaction commits</li>
 * </ol>
 * Since the value is still the same from the perspective of the first transaction (it has changed from
 * "A" to "B" back to "A" again) the question remains what to do. Should to be a problem or not.
 * <p/>
 * The stm object do dirty checks, so the second transaction that does the A->B and B-A doesn't write
 * any change because reference is not marked as dirty.
 * <p/>
 * For more information see:
 * http://en.wikipedia.org/wiki/ABA_problem
 *
 * @author Peter Veentjer.
 */
public class AbaProblemIsNotDetectedTest {
    private static final int A = 1;
    private static final int B = 2;
    private static final int C = 3;

    private AlphaStm stm;
    private IntRef ref;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    public AlphaTransaction startUpdateTransaction() {
        AlphaTransaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();
        setThreadLocalTransaction(t);
        return t;
    }


    @Test
    public void test() {
        ref = new IntRef(A);

        AlphaTransaction t1 = startUpdateTransaction();
        IntRefTranlocal r1 = (IntRefTranlocal) t1.openForWrite(ref);

        AlphaTransaction t2 = startUpdateTransaction();
        IntRefTranlocal r2 = (IntRefTranlocal) t2.openForWrite(ref);
        ref.set(r2, B);
        ref.set(r2, A);
        t2.commit();

        ref.set(r1, C);
        t1.commit();
    }
}
