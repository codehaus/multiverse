package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class SerializedTest {
    private BetaStm stm;

    @Test
    public void setUp() {
        stm = (BetaStm) getGlobalStmInstance();
    }

    @Test
    @Ignore
    public void test() {
    }
}