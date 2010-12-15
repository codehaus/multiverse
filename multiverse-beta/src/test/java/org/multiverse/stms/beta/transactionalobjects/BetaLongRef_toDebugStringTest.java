package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class BetaLongRef_toDebugStringTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void test() {
        BetaLongRef ref = newLongRef(stm);
        String s = ref.toDebugString();
        assertEquals("Ref{orec=FastOrec(hasCommitLock=false, hasUpdateLock=false, surplus=0, " +
                "isReadBiased=false, readonlyCount=0), version=1, value=0, hasListeners=false)",s);
    }
}
