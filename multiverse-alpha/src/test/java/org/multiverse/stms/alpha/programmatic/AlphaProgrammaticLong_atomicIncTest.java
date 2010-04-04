package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_atomicIncTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoTransactionOnThreadLocal() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(10);

        long version = stm.getVersion();
        ref.atomicInc(1);

        assertEquals(11, ref.atomicGet());
        assertEquals(version + 1, stm.getVersion());
        assertEquals(version + 1, ref.___load().getWriteVersion());
    }

    @Test
    public void whenTransactionOnThreadLocal_thenItIsIgnored() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(10);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setAutomaticReadTracking(true)
                .build()
                .start();

        setThreadLocalTransaction(tx);

        long version = stm.getVersion();
        ref.atomicInc(1);

        assertEquals(11, ref.atomicGet());
        assertEquals(version + 1, stm.getVersion());
        assertEquals(version + 1, ref.___load().getWriteVersion());
    }
}
