package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_getTest {

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

    }
}
