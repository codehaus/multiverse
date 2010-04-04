package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_setTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void after() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoTransactionAvailable_thenCallExecutedAtomically() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        long version = stm.getVersion();
        ref.set(20);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(20, ref.atomicGet());
        //assertEquals(version + 1, ref.atomicGetVersion());
        assertNull(ref.___getLockOwner());
    }

    @Test
    @Ignore
    public void whenNoChange() {

    }

    @Test
    @Ignore
    public void whenLocked() {

    }

    @Test
    @Ignore
    public void whenTransactionAvailable_thenTransactionUsed() {

    }
}
