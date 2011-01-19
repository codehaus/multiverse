package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class SerializedTest {
    private GammaStm stm;

    @Test
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
    }

    @Test
    @Ignore
    public void test() {
    }
}
