package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.stms.alpha.AlphaStm;
import static org.multiverse.stms.alpha.AlphaStmConfig.createFastConfig;

import java.util.concurrent.TimeUnit;

public class IntRefPerformanceLongTest {

    private int count = 10 * 1000 * 1000;

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm(createFastConfig());
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
        if (stm.getProfiler() != null) {
            //stm.getProfiler().print();
        }
    }

    @Test
    public void test() {
        IntRef ref = new IntRef();

        long startNs = System.nanoTime();

        for (int k = 0; k < count; k++) {
            ref.inc();
        }

        assertEquals(count, ref.get());
        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (count * 1.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", transactionPerSecond);
    }
}
