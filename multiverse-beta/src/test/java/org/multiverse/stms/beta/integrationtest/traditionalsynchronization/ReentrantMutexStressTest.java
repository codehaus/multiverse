package org.multiverse.stms.beta.integrationtest.traditionalsynchronization;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ReentrantMutexStressTest {
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
