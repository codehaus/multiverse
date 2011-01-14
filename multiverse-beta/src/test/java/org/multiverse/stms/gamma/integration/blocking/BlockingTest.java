package org.multiverse.stms.gamma.integration.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.LongRefAwaitThread;

import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class BlockingTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenDesiredValueNotAvailable_thenThreadBlocks() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        LongRefAwaitThread t = new LongRefAwaitThread(ref, 1);
        t.start();

        sleepMs(1000);
        assertAlive(t);
    }

    @Test
    public void whenDesiredValueStillNotAvailable_thenThreadBlocks() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        LongRefAwaitThread t = new LongRefAwaitThread(ref, 2);
        t.start();

        sleepMs(2000);
        ref.atomicSet(1);

        sleepMs(1000);
        assertAlive(t);
    }
}
