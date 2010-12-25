package org.multiverse.stms.beta.integrationtest.traditionalsynchronization;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A Stresstest that sees if the stm can be used to create a readwritelock that is not reentrant.
 *
 * @author Peter Veentjer.
 */
public class NonReentrantReadWriteLockStressTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = (BetaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void test() {

    }
}
