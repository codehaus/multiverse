package org.multiverse.stms.beta.integrationtest.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefAwaitThread;

import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class BlockingTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = (BetaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenDesiredValueNotAvailable_thenThreadBlocks() {
        BetaLongRef ref = stm.getDefaultRefFactory().newLongRef(0);

        LongRefAwaitThread t = new LongRefAwaitThread(ref, 1);
        t.start();

        sleepMs(1000);
        assertAlive(t);
    }

    @Test
    public void whenDesiredValueStillNotAvailable_thenThreadBlocks() {
        BetaLongRef ref = stm.getDefaultRefFactory().newLongRef(0);

        LongRefAwaitThread t = new LongRefAwaitThread(ref, 2);
        t.start();

        sleepMs(10000);
        ref.atomicSet(1);

        sleepMs(1000);
        assertAlive(t);
    }
}
