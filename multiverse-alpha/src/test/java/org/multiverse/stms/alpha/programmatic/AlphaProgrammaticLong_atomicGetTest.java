package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_atomicGetTest {
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
    public void test() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        long version = stm.getVersion();
        long value = ref.atomicGet();

        assertEquals(value, 10);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNoCommitExecutedBefore() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted();

        long version = stm.getVersion();
        long value = ref.atomicGet();

        assertEquals(0, value);
        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
    }

    @Test
    public void whenTransactionRunning_thenItsIgnored() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);


        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .start();

        setThreadLocalTransaction(tx);
        ref.inc(tx, 2);

        long version = stm.getVersion();
        long value = ref.atomicGet();
        assertEquals(10, value);
        assertEquals(version, stm.getVersion());
    }


}
