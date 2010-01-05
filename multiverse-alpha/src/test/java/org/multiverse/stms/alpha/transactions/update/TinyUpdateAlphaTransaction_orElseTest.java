package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.transactions.OptimalSize;

public class TinyUpdateAlphaTransaction_orElseTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public TinyUpdateAlphaTransaction startSutTransaction() {
        TinyUpdateAlphaTransaction.Config config = new TinyUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.restartBackoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount,
                stmConfig.commitLockPolicy, true, optimalSize,true,true, true, true);
        return new TinyUpdateAlphaTransaction(config);
    }

    @Test
    @Ignore
    public void test(){

    }
}
