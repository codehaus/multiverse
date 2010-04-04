package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_commutingIncWithTransactionTest {
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
    public void whenTransactionNull_thenNullPointerException() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        long version = stm.getVersion();
        try {
            ref.commutingInc(null, 20);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals(1, ref.atomicGet());
    }

    @Test
    @Ignore
    public void test() {

    }
}
